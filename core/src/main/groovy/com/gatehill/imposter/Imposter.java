package com.gatehill.imposter;

import com.gatehill.imposter.plugin.*;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_SUFFIX;
import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);

    private final ImposterConfig imposterConfig;
    private final Module[] bootstrapModules;
    private final PluginManager pluginManager;

    public Imposter(ImposterConfig imposterConfig, Module... bootstrapModules) {
        this(imposterConfig, bootstrapModules, new PluginManager());
    }

    public Imposter(ImposterConfig imposterConfig, Module[] bootstrapModules, PluginManager pluginManager) {
        this.imposterConfig = imposterConfig;
        this.bootstrapModules = bootstrapModules;
        this.pluginManager = pluginManager;
    }

    public void start() {
        LOGGER.info("Starting mock engine");

        // load config
        processConfiguration();
        final Map<String, List<File>> pluginConfigs = loadPluginConfigs(imposterConfig.getConfigDirs());

        // prepare plugins
        final List<PluginDependencies> dependencies = preparePluginsFromConfig(imposterConfig.getPluginClassNames(), pluginConfigs)
                .stream()
                .filter(deps -> nonNull(deps.getRequiredModules()))
                .collect(Collectors.toList());

        final List<Module> allModules = newArrayList(bootstrapModules);
        allModules.add(new ImposterModule(imposterConfig, pluginManager));
        dependencies.forEach(deps -> allModules.addAll(deps.getRequiredModules()));

        // inject dependencies
        final Injector injector = InjectorUtil.create(allModules.toArray(new Module[0]));
        injector.injectMembers(this);
        registerPlugins(injector);

        configurePlugins(pluginConfigs);
    }

    private void processConfiguration() {
        imposterConfig.setServerUrl(buildServerUrl().toString());

        final String[] configDirs = imposterConfig.getConfigDirs();

        // resolve relative config paths
        for (int i = 0; i < configDirs.length; i++) {
            if (configDirs[i].startsWith("./")) {
                configDirs[i] = Paths.get(System.getProperty("user.dir"), configDirs[i].substring(2)).toString();
            }
        }
    }

    private URI buildServerUrl() {
        // might be set explicitly
        final Optional<String> explicitUrl = ofNullable(imposterConfig.getServerUrl());
        if (explicitUrl.isPresent()) {
            return URI.create(explicitUrl.get());
        }

        // build based on configuration
        final String scheme = (imposterConfig.isTlsEnabled() ? "https" : "http") + "://";
        final String host = (BIND_ALL_HOSTS.equals(imposterConfig.getHost()) ? "localhost" : imposterConfig.getHost());

        final String port;
        if ((imposterConfig.isTlsEnabled() && 443 == imposterConfig.getListenPort())
                || (!imposterConfig.isTlsEnabled() && 80 == imposterConfig.getListenPort())) {
            port = "";
        } else {
            port = ":" + imposterConfig.getListenPort();
        }

        return URI.create(scheme + host + port);
    }

    private List<PluginDependencies> preparePluginsFromConfig(String[] pluginClassNames, Map<String, List<File>> pluginConfigs) {
        final List<PluginDependencies> dependencies = newArrayList();

        ofNullable(pluginClassNames).ifPresent(classNames ->
                Arrays.stream(classNames).forEach(this::registerPluginClass));

        dependencies.addAll(pluginManager.getPluginClasses().stream()
                .map(this::examinePlugin)
                .collect(Collectors.toList()));

        findUnregisteredProviders().forEach(providerClass -> {
            pluginManager.registerProvider(providerClass);
            final PluginProvider pluginProvider = createPluginProvider(providerClass);
            final String[] provided = pluginProvider.providePlugins(imposterConfig, pluginConfigs);
            LOGGER.debug("{} plugins provided by {}", provided.length, providerClass.getCanonicalName());

            // recurse for new providers
            if (provided.length > 0) {
                dependencies.addAll(preparePluginsFromConfig(provided, pluginConfigs));
            }
        });

        return dependencies;
    }

    @SuppressWarnings("unchecked")
    private void registerPluginClass(String className) {
        try {
            if (pluginManager.registerClass((Class<? extends Plugin>) Class.forName(className))) {
                LOGGER.debug("Registered plugin {}", className);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register plugin: " + className, e);
        }
    }

    /**
     * Examine the plugin for any required dependencies.
     *
     * @param pluginClass the plugin to examine
     * @return the plugin's dependencies
     */
    private PluginDependencies examinePlugin(Class<? extends Plugin> pluginClass) {
        final PluginDependencies registration = new PluginDependencies();

        final RequireModules moduleAnnotation = pluginClass.getAnnotation(RequireModules.class);
        if (null != moduleAnnotation && moduleAnnotation.value().length > 0) {
            registration.setRequiredModules(instantiateModules(moduleAnnotation));
        }

        return registration;
    }

    /**
     * @return any {@link PluginProvider}s not yet registered with the plugin manager
     */
    @SuppressWarnings("unchecked")
    private List<Class<PluginProvider>> findUnregisteredProviders() {
        return pluginManager.getPluginClasses().stream()
                .filter(pluginClass -> pluginClass.isAssignableFrom(PluginProvider.class))
                .map(pluginClass -> (Class<PluginProvider>) pluginClass.asSubclass(PluginProvider.class))
                .filter(providerClass -> !pluginManager.isProviderRegistered(providerClass))
                .collect(Collectors.toList());
    }

    private PluginProvider createPluginProvider(Class<PluginProvider> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating plugin provider: " + providerClass.getCanonicalName(), e);
        }
    }

    /**
     * Instantiate all plugins and register them with the plugin manager.
     *
     * @param injector the injector from which the plugins can be instantiated
     */
    private void registerPlugins(Injector injector) {
        pluginManager.getPluginClasses().forEach(pluginClass -> {
            pluginManager.registerInstance(injector.getInstance(pluginClass));
        });

        final int pluginCount = pluginManager.getPlugins().size();
        if (pluginCount > 0) {
            LOGGER.info("Loaded {} plugins", pluginCount);
        } else {
            throw new IllegalStateException("No plugins were loaded");
        }
    }

    private List<Module> instantiateModules(RequireModules moduleAnnotation) {
        final List<Module> modules = newArrayList();
        for (Class<? extends Module> moduleClass : moduleAnnotation.value()) {
            try {
                modules.add(moduleClass.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return modules;
    }

    /**
     * Send config to plugins.
     *
     * @param pluginConfigs configurations keyed by plugin
     */
    private void configurePlugins(Map<String, List<File>> pluginConfigs) {
        pluginManager.getPlugins().stream()
                .filter(plugin -> plugin instanceof ConfigurablePlugin)
                .map(plugin -> (ConfigurablePlugin) plugin)
                .forEach(plugin -> {
                    final List<File> configFiles = ofNullable(pluginConfigs.get(plugin.getClass().getCanonicalName()))
                            .orElse(Collections.emptyList());
                    plugin.loadConfiguration(configFiles);
                });
    }

    private Map<String, List<File>> loadPluginConfigs(String[] configDirs) {
        int configCount = 0;

        // read all config files
        final Map<String, List<File>> allPluginConfigs = Maps.newHashMap();
        for (String configDir : configDirs) {
            try {
                final File[] configFiles = ofNullable(new File(configDir).listFiles((dir, name) -> name.endsWith(CONFIG_FILE_SUFFIX)))
                        .orElse(new File[0]);

                for (File configFile : configFiles) {
                    LOGGER.debug("Loading configuration file: {}", configFile);
                    configCount++;

                    final BaseConfig config = JSON_MAPPER.readValue(configFile, BaseConfig.class);
                    config.setParentDir(configFile.getParentFile());

                    List<File> pluginConfigs = allPluginConfigs.get(config.getPluginClass());
                    if (Objects.isNull(pluginConfigs)) {
                        pluginConfigs = newArrayList();
                        allPluginConfigs.put(config.getPluginClass(), pluginConfigs);
                    }

                    pluginConfigs.add(configFile);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LOGGER.info("Loaded {} plugin configuration files from: {}",
                configCount, Arrays.toString(configDirs));

        return allPluginConfigs;
    }
}
