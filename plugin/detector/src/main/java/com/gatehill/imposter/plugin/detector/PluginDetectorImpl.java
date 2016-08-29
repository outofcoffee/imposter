package com.gatehill.imposter.plugin.detector;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginProvider;
import io.vertx.ext.web.Router;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides the plugin class names present in the configuration files.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PluginDetectorImpl implements Plugin, PluginProvider {

    @Override
    public void configureRoutes(Router router) {
        // no op
    }

    @Override
    public String[] providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs) {
        final Set<String> pluginClassNames = pluginConfigs.keySet().stream()
                .distinct().collect(Collectors.toSet());

        return pluginClassNames.toArray(new String[pluginClassNames.size()]);
    }
}
