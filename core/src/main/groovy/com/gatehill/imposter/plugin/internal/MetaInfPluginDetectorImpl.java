package com.gatehill.imposter.plugin.internal;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginInfo;
import com.gatehill.imposter.plugin.PluginProvider;
import com.gatehill.imposter.util.MetaUtil;
import io.vertx.ext.web.Router;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Provides the plugin class names present in the metadata information ('META-INF') files.
 * <p>
 * Checks for a list of comma-separated plugin names or fully qualified classes.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@PluginInfo("meta-detector")
public class MetaInfPluginDetectorImpl implements Plugin, PluginProvider {
    @Override
    public void configureRoutes(Router router) {
        // no op
    }

    @Override
    public List<String> providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs) {
        return ofNullable(MetaUtil.readMetaProperties().getProperty("plugins"))
                .map(plugin -> plugin.split(","))
                .map(elements -> (List<String>) newArrayList(elements))
                .orElse(emptyList());
    }
}
