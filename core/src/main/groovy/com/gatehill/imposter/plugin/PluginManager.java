package com.gatehill.imposter.plugin;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PluginManager {
    private static final Logger LOGGER = LogManager.getLogger(PluginManager.class);

    /**
     * The base package to scan recursively for plugins.
     */
    private static final String PLUGIN_BASE_PACKAGE = "com.gatehill.imposter.plugin";

    private final Map<String, String> classpathPlugins = new HashMap<>();
    private final Set<Class<? extends Plugin>> pluginClasses = Sets.newHashSet();
    private final Set<Class<? extends PluginProvider>> providers = Sets.newHashSet();
    private final Map<String, Plugin> plugins = Maps.newHashMap();
    private boolean hasScannedForPlugins;

    /**
     * Determines the plugin class if it matches its short name, otherwise assumes
     * the plugin is a fully qualified class name.
     *
     * @param plugin the plugin short name or fully qualified class name
     * @return the fully qualified plugin class name
     */
    public String determinePluginClass(String plugin) {
        if (!hasScannedForPlugins) {
            synchronized (classpathPlugins) {
                if (!hasScannedForPlugins) { // double-guard
                    classpathPlugins.putAll(discoverClasspathPlugins());
                    hasScannedForPlugins = true;
                }
            }
        }
        return ofNullable(classpathPlugins.get(plugin)).orElse(plugin);
    }

    /**
     * Finds plugins on the classpath annotated with {@link PluginInfo}.
     *
     * @return a map of plugin short names to full qualified class names
     */
    private Map<String, String> discoverClasspathPlugins() {
        final Map<String, String> pluginClasses = new HashMap<>();
        try (ScanResult result = new ClassGraph().enableClassInfo().enableAnnotationInfo()
                .whitelistPackages(PLUGIN_BASE_PACKAGE).scan()) {

            final ClassInfoList pluginClassInfos = result
                    .getClassesImplementing(Plugin.class.getName())
                    .filter(classInfo -> classInfo.hasAnnotation(PluginInfo.class.getName()));

            for (ClassInfo pluginClassInfo : pluginClassInfos) {
                try {
                    final Object pluginName = pluginClassInfo
                            .getAnnotationInfo().get(0)
                            .getParameterValues().get(0).getValue();

                    pluginClasses.put((String) pluginName, pluginClassInfo.getName());
                } catch (Exception e) {
                    LOGGER.warn("Error reading plugin class info for: {}", pluginClassInfo.getName(), e);
                }
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Annotated plugins on classpath: {}", pluginClasses);
        }
        return pluginClasses;
    }

    public boolean registerClass(Class<? extends Plugin> plugin) {
        return pluginClasses.add(plugin);
    }

    public Collection<Class<? extends Plugin>> getPluginClasses() {
        return Collections.unmodifiableCollection(pluginClasses);
    }

    public void registerInstance(Plugin instance) {
        plugins.put(instance.getClass().getCanonicalName(), instance);
    }

    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    @SuppressWarnings("unchecked")
    public <P extends Plugin> P getPlugin(String pluginClassName) {
        return (P) plugins.get(pluginClassName);
    }

    public void registerProvider(Class<? extends PluginProvider> provider) {
        providers.add(provider);
    }

    public boolean isProviderRegistered(Class<? extends PluginProvider> provider) {
        return providers.contains(provider);
    }

    /**
     * Registers plugin providers and discovers dependencies from configuration.
     *
     * @param imposterConfig application configuration
     * @param plugins        configured plugins
     * @param pluginConfigs  plugin configurations
     * @return list of dependencies
     */
    public List<PluginDependencies> preparePluginsFromConfig(
            ImposterConfig imposterConfig,
            List<String> plugins,
            Map<String, List<File>> pluginConfigs) {

        final List<PluginDependencies> dependencies = newArrayList();

        // prepare plugins
        ofNullable(plugins).orElse(emptyList()).stream()
                .map(this::determinePluginClass)
                .forEach(this::registerPluginClass);

        dependencies.addAll(getPluginClasses().stream()
                .map(this::examinePlugin)
                .collect(Collectors.toList()));

        findUnregisteredProviders().forEach(providerClass -> {
            registerProvider(providerClass);
            final PluginProvider pluginProvider = createPluginProvider(providerClass);
            final List<String> provided = pluginProvider.providePlugins(imposterConfig, pluginConfigs);
            LOGGER.debug("{} plugin(s) provided by: {}", provided.size(), pluginProvider.getName());

            // recurse for new providers
            if (provided.size() > 0) {
                dependencies.addAll(preparePluginsFromConfig(imposterConfig, provided, pluginConfigs));
            }
        });

        return dependencies;
    }

    @SuppressWarnings("unchecked")
    private void registerPluginClass(String className) {
        try {
            final Class<? extends Plugin> clazz = (Class<? extends Plugin>) Class.forName(className);
            if (registerClass(clazz)) {
                final String pluginName = PluginMetadata.getPluginName(clazz);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Registered plugin: {} with class: {}", pluginName, className);
                } else {
                    LOGGER.debug("Registered plugin: {}", pluginName);
                }
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
     * @return any {@link PluginProvider}s not yet registered with the plugin manager
     */
    @SuppressWarnings("unchecked")
    private List<Class<PluginProvider>> findUnregisteredProviders() {
        return getPluginClasses().stream()
                .filter(PluginProvider.class::isAssignableFrom)
                .map(pluginClass -> (Class<PluginProvider>) pluginClass.asSubclass(PluginProvider.class))
                .filter(providerClass -> !isProviderRegistered(providerClass))
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
    public void registerPlugins(Injector injector) {
        getPluginClasses().forEach(pluginClass ->
                registerInstance(injector.getInstance(pluginClass)));

        final int pluginCount = getPlugins().size();
        if (pluginCount > 0) {
            final String pluginNames = getPlugins().stream()
                    .map(Plugin::getName)
                    .collect(Collectors.joining(", ", "[", "]"));
            LOGGER.info("Loaded {} plugin(s): {}", pluginCount, pluginNames);
        } else {
            throw new IllegalStateException("No plugins were loaded");
        }
    }

    /**
     * Send config to plugins.
     *
     * @param pluginConfigs configurations keyed by plugin
     */
    public void configurePlugins(Map<String, List<File>> pluginConfigs) {
        getPlugins().stream()
                .filter(plugin -> plugin instanceof ConfigurablePlugin)
                .map(plugin -> (ConfigurablePlugin) plugin)
                .forEach(plugin -> {
                    final List<File> configFiles = ofNullable(pluginConfigs.get(plugin.getClass().getCanonicalName()))
                            .orElse(emptyList());
                    plugin.loadConfiguration(configFiles);
                });
    }
}
