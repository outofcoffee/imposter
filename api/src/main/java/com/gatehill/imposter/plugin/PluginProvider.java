package com.gatehill.imposter.plugin;

import com.gatehill.imposter.ImposterConfig;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface PluginProvider {
    /**
     * Provide class names of plugins.
     *
     * @param imposterConfig core configuration
     * @param pluginConfigs  plugin configurations
     * @return plugins
     */
    String[] providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs);
}
