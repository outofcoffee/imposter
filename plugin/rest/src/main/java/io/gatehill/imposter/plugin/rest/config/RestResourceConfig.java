package io.gatehill.imposter.plugin.rest.config;

import io.gatehill.imposter.plugin.config.resource.ContentTypedResourceConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestResourceConfig extends ContentTypedResourceConfigImpl implements MethodResourceConfig {
    private ResourceConfigType type;
    private ResourceMethod method;

    public ResourceConfigType getType() {
        return type;
    }

    @Override
    public ResourceMethod getMethod() {
        return method;
    }
}
