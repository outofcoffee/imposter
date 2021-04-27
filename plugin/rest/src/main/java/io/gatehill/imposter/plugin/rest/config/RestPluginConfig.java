package io.gatehill.imposter.plugin.rest.config;

import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends ContentTypedPluginConfigImpl implements MethodResourceConfig {
    private List<RestPluginResourceConfig> resources;
    private ResourceMethod method;

    public List<RestPluginResourceConfig> getResources() {
        return resources;
    }

    @Override
    public ResourceMethod getMethod() {
        return method;
    }
}
