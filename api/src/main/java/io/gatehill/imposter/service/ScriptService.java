package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.script.InternalResponseBehavior;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptService {
    /**
     * Execute the script and read response behaviour.
     *
     * @param pluginConfig   the plugin configuration
     * @param resourceConfig the resource configuration
     * @param bindings       the script engine bindings
     * @return the response behaviour
     */
    InternalResponseBehavior executeScript(PluginConfig pluginConfig, ResourceConfig resourceConfig, Map<String, Object> bindings);
}
