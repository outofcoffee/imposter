package io.gatehill.imposter.plugin.detector;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.PluginProvider;
import io.vertx.ext.web.Router;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides the plugin class names present in the configuration files.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@PluginInfo("config-detector")
public class ConfigPluginDetectorImpl implements Plugin, PluginProvider {
    @Override
    public void configureRoutes(Router router) {
        // no op
    }

    @Override
    public List<String> providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs) {
        return pluginConfigs.keySet().stream()
                .distinct()
                .collect(Collectors.toList());
    }
}
