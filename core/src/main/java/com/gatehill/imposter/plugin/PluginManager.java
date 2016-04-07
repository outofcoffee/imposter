package com.gatehill.imposter.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PluginManager {
    private List<Class<? extends Plugin>> pluginClasses = Lists.newArrayList();
    private Map<String, Plugin> plugins = Maps.newHashMap();

    public void registerClass(Class<? extends Plugin> plugin) {
        pluginClasses.add(plugin);
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
}
