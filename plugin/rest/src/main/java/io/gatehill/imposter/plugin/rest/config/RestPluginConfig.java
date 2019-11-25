package io.gatehill.imposter.plugin.rest.config;

import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends ContentTypedPluginConfigImpl implements MethodResourceConfig {
    private List<RestResourceConfig> resources;
    private ResourceMethod method;

    public List<RestResourceConfig> getResources() {
        return resources;
    }

    @Override
    public ResourceMethod getMethod() {
        return method;
    }
}
