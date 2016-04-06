package com.gatehill.imposter.plugin;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * @author pcornish
 */
public class PluginManager {
    private List<Class<? extends Plugin>> pluginClasses = Lists.newArrayList();
    private List<Plugin> plugins = Lists.newArrayList();

    public void registerClass(Class<? extends Plugin> plugin) {
        pluginClasses.add(plugin);
    }

    public List<Class<? extends Plugin>> getPluginClasses() {
        return Collections.unmodifiableList(pluginClasses);
    }

    public void registerInstance(Plugin instance) {
        plugins.add(instance);
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }
}
