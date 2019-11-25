package io.gatehill.imposter.plugin;

import com.google.inject.Module;

import java.util.List;

/**
 * Represents the dependencies of a plugin.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PluginDependencies {
    private List<Module> requiredModules;

    public void setRequiredModules(List<Module> requiredModules) {
        this.requiredModules = requiredModules;
    }

    public List<Module> getRequiredModules() {
        return requiredModules;
    }
}
