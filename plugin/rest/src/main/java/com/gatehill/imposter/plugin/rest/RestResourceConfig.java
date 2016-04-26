package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.plugin.config.ResourceConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestResourceConfig extends ResourceConfig {
    private ResourceConfigType type;

    public ResourceConfigType getType() {
        return type;
    }
}
