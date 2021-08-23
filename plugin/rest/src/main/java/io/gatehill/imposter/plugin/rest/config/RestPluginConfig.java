package io.gatehill.imposter.plugin.rest.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends ContentTypedPluginConfigImpl implements MethodResourceConfig, ResourcesHolder<RestPluginResourceConfig> {
    @JsonProperty("resources")
    private List<RestPluginResourceConfig> resources;

    @JsonProperty("method")
    private ResourceMethod method;

    @JsonProperty("defaultsFromRootResponse")
    private boolean defaultsFromRootResponse;

    public List<RestPluginResourceConfig> getResources() {
        return resources;
    }

    @Override
    public ResourceMethod getMethod() {
        return method;
    }

    @Override
    public boolean isDefaultsFromRootResponse() {
        return defaultsFromRootResponse;
    }
}
