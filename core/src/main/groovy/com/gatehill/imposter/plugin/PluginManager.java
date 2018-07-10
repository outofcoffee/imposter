package com.gatehill.imposter.plugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PluginManager {
    private final Set<Class<? extends Plugin>> pluginClasses = Sets.newHashSet();
    private final Set<Class<? extends PluginProvider>> providers = Sets.newHashSet();
    private final Map<String, Plugin> plugins = Maps.newHashMap();

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

    public boolean removeClass(Class<? extends Plugin> plugin) {
        if (pluginClasses.contains(plugin)) {
            pluginClasses.remove(plugin);
            return true;
        }
        return false;

    }
}
