package com.gatehill.imposter.service;

import com.gatehill.imposter.plugin.config.ResourceConfig;
import com.gatehill.imposter.script.MutableResponseBehaviour;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptService {
    /**
     * Execute the script and read response behaviour.
     *
     * @param config  the plugin configuration
     * @param bindings the script engine bindings
     * @return the response behaviour
     */
    MutableResponseBehaviour executeScript(ResourceConfig config, Map<String, Object> bindings);
}
