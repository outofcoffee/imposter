package com.gatehill.imposter.plugin.rest.config;

import com.gatehill.imposter.plugin.config.resource.ContentTypedResourceConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestResourceConfig extends ContentTypedResourceConfigImpl {
    private ResourceConfigType type;
    private ResourceMethod method;

    public ResourceConfigType getType() {
        return type;
    }

    public ResourceMethod getMethod() {
        return method;
    }
}
