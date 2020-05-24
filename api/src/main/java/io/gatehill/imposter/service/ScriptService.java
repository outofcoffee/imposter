package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.script.ScriptedResponseBehavior;
import io.gatehill.imposter.script.RuntimeContext;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptService {
    /**
     * Execute the script and read response behaviour.
     *
     * @param pluginConfig   the plugin configuration
     * @param resourceConfig the resource configuration
     * @param runtimeContext the script engine runtime context
     * @return the response behaviour
     */
    ScriptedResponseBehavior executeScript(PluginConfig pluginConfig, ResourceConfig resourceConfig, RuntimeContext runtimeContext);
}
