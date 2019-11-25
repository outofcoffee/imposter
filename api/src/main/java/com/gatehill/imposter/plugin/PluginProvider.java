package com.gatehill.imposter.plugin;

import com.gatehill.imposter.ImposterConfig;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Provides class names of plugins to load.
 * <p>
 * Note that plugin providers are instantiated before dependency injection is available.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface PluginProvider extends PluginMetadata {
    /**
     * Provide class names of plugins.
     *
     * @param imposterConfig core configuration
     * @param pluginConfigs  plugin configurations
     * @return plugins
     */
    List<String> providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs);
}
