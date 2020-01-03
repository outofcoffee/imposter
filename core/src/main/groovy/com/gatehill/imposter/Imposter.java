package com.gatehill.imposter;

import com.gatehill.imposter.plugin.PluginDependencies;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.util.ConfigUtil;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

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
        final Map<String, List<File>> pluginConfigs = ConfigUtil.loadPluginConfigs(pluginManager, imposterConfig.getConfigDirs());


        // prepare plugins
        final List<String> pluginClassNames = Arrays.stream(imposterConfig.getPlugins())
                .map(pluginManager::determinePluginClass).collect(Collectors.toList());

        final List<PluginDependencies> dependencies = pluginManager.preparePluginsFromConfig(imposterConfig, pluginClassNames, pluginConfigs)
                .stream()
                .filter(deps -> nonNull(deps.getRequiredModules()))
                .collect(Collectors.toList());

        final List<Module> allModules = newArrayList(bootstrapModules);
        allModules.add(new ImposterModule(imposterConfig, pluginManager));
        dependencies.forEach(deps -> allModules.addAll(deps.getRequiredModules()));


        // inject dependencies
        final Injector injector = InjectorUtil.create(allModules.toArray(new Module[0]));
        injector.injectMembers(this);
        pluginManager.registerPlugins(injector);
        pluginManager.configurePlugins(pluginConfigs);

        FileWatcher fw = new FileWatcher(this);
        Thread t = new Thread(fw);
        t.start();
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


    @SuppressWarnings("unchecked")
//    private void instantiatePlugins(String[] plugins, Map<String, List<File>> pluginConfigs) {
//        instantiatePluginsFromConfig(plugins, pluginConfigs);
//
//        final int pluginCount = pluginManager.getPlugins().size();
//        if (pluginCount > 0) {
//            LOGGER.info("Loaded {} plugins", pluginCount);
//        } else {
//            throw new RuntimeException("No plugins were loaded");
//        }
//    }
//
//    private void instantiatePluginsFromConfig(String[] pluginClassNames, Map<String, List<File>> pluginConfigs) {
//        ofNullable(pluginClassNames).ifPresent(classNames ->
//                Arrays.stream(classNames).forEach(this::registerPluginClass));
//
//        pluginManager.getPluginClasses().forEach(this::registerPlugin);
//
//        final List<PluginProvider> newProviders = pluginManager.getPlugins().stream()
//                .filter(plugin -> plugin instanceof PluginProvider)
//                .map(plugin -> ((PluginProvider) plugin))
//                .filter(provider -> pluginManager.isProviderRegistered(provider.getClass()))
//                .collect(toList());
//
//        // recurse for any new providers
//        newProviders.forEach(provider -> {
//            pluginManager.registerProvider(provider.getClass());
//            final String[] provided = provider.providePlugins(imposterConfig, pluginConfigs);
//            LOGGER.debug("{} plugins provided by {}", provided.length, provider.getClass().getCanonicalName());
//            instantiatePluginsFromConfig(provided, pluginConfigs);
//        });
//    }
//
//    @SuppressWarnings("unchecked")
//    private void registerPluginClass(String className) {
//        try {
//            if (pluginManager.registerClass((Class<? extends Plugin>) Class.forName(className))) {
//                LOGGER.debug("Registered plugin {}", className);
//            }
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void registerPlugin(Class<? extends Plugin> pluginClass) {
//        final Injector pluginInjector;
//
//        final RequireModules moduleAnnotation = pluginClass.getAnnotation(RequireModules.class);
//        if (null != moduleAnnotation && moduleAnnotation.value().length > 0) {
//            pluginInjector = injector.createChildInjector(instantiateModules(moduleAnnotation));
//        } else {
//            pluginInjector = injector;
//        }
//
//        pluginManager.registerInstance(pluginInjector.getInstance(pluginClass));
//    }
//
//    private List<Module> instantiateModules(RequireModules moduleAnnotation) {
//        final List<Module> modules = Lists.newArrayList();
//
//        for (Class<? extends Module> moduleClass : moduleAnnotation.value()) {
//            try {
//                modules.add(moduleClass.newInstance());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        return modules;
//    }
//
//    /**
//     * Send config to plugins.
//     *
//     * @param pluginConfigs configurations keyed by plugin
//     */
//    private void configurePlugins(Map<String, List<File>> pluginConfigs) {
//        pluginManager.getPlugins().stream()
//                .filter(plugin -> plugin instanceof ConfigurablePlugin)
//                .map(plugin -> (ConfigurablePlugin) plugin)
//                .forEach(plugin -> {
//                    final List<File> configFiles = ofNullable(pluginConfigs.get(plugin.getClass().getCanonicalName()))
//                            .orElse(Collections.emptyList());
//                    plugin.loadConfiguration(configFiles);
//                });
//    }
//
//    private Map<String, List<File>> loadPluginConfigs(String[] configDirs) {
//        int configCount = 0;
//
//        // read all config files
//        final Map<String, List<File>> allPluginConfigs = Maps.newHashMap();
//        for (String configDir : configDirs) {
//            try {
//                final File[] configFiles = ofNullable(new File(configDir).listFiles((dir, name) -> name.endsWith(CONFIG_FILE_SUFFIX)))
//                        .orElse(new File[0]);
//
//                for (File configFile : configFiles) {
//                    LOGGER.debug("Loading configuration file: {}", configFile);
//                    configCount++;
//
//                    final BaseConfig config = MAPPER.readValue(configFile, BaseConfig.class);
//                    config.setParentDir(configFile.getParentFile());
//
//                    List<File> pluginConfigs = allPluginConfigs.get(config.getPluginClass());
//                    if (Objects.isNull(pluginConfigs)) {
//                        pluginConfigs = newArrayList();
//                        allPluginConfigs.put(config.getPluginClass(), pluginConfigs);
//                    }
//
//                    pluginConfigs.add(configFile);
//                }
//
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        LOGGER.info("Loaded {} plugin configuration files from: {}",
//                configCount, Arrays.toString(configDirs));
//
//        return allPluginConfigs;
//    }

    class FileWatcher implements Runnable {
        Map<String, WatchService> watchServices = new HashMap<>();
        private Imposter imposter;

        FileWatcher(Imposter imposter) {
            this.imposter = imposter;
            Arrays.stream(imposterConfig.getConfigDirs()).forEachOrdered(config -> {
                Path path = Paths.get(config);

                try (Stream<Path> paths = Files.walk(path)) {

                     List<Path> yamls = paths.filter(Files::isRegularFile)
                            .filter(filePath -> {
                                try {
                                    return (filePath.toFile().getCanonicalPath().endsWith("yaml") ||
                                            filePath.toFile().getCanonicalPath().endsWith("yml"));
                                } catch (IOException io) {
                                    System.out.println(io.getLocalizedMessage());
                                    return false;
                                }
                            }).collect(toList());


                    yamls.forEach(p -> {
                        try{
                            String actualPath = p.toFile().getCanonicalPath();
                            watchServices.put(actualPath, Paths.get(actualPath).getParent().getFileSystem().newWatchService());
                        } catch(IOException io){
                            System.out.println(io.getLocalizedMessage());
                        }
                    });

                    watchServices.forEach((s,w)-> {
                        try {
                            Paths.get(s).getParent().register(w, StandardWatchEventKinds.ENTRY_MODIFY);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException io) {
                    System.out.println(io.getLocalizedMessage());
                }
            });
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                watchServices.forEach((s,w) -> {
                    WatchKey watchKey;
                    try {
                        watchKey = w.take();
                        if (watchKey != null) {
                            watchKey.pollEvents().forEach(event -> System.out.println("File changed reloading plugins and registering new changes"));

                            final Map<String, List<File>> pluginConfigs = ConfigUtil.loadPluginConfigs(pluginManager, imposterConfig.getConfigDirs());

                            pluginManager.getPluginClasses().forEach(pluginManager::removeClass);

                            // prepare plugins
                            final List<String> pluginClassNames = Arrays.stream(imposterConfig.getPlugins())
                                    .map(pluginManager::determinePluginClass).collect(Collectors.toList());

                            final List<PluginDependencies> dependencies = pluginManager.preparePluginsFromConfig(imposterConfig, pluginClassNames, pluginConfigs)
                                    .stream()
                                    .filter(deps -> nonNull(deps.getRequiredModules()))
                                    .collect(Collectors.toList());

                            final List<Module> allModules = newArrayList(bootstrapModules);
                            allModules.add(new ImposterModule(imposterConfig, pluginManager));
                            dependencies.forEach(deps -> allModules.addAll(deps.getRequiredModules()));


                            // inject dependencies
                            final Injector injector = InjectorUtil.create(allModules.toArray(new Module[0]));
                            injector.injectMembers(imposter);
                            pluginManager.registerPlugins(injector);
                            pluginManager.configurePlugins(pluginConfigs);

                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
