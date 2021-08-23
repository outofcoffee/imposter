package io.gatehill.imposter.plugin.test;

import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestPluginConfig extends PluginConfigImpl implements ResourcesHolder<RestResourceConfig> {
    private List<RestResourceConfig> resources;
    private boolean defaultsFromRootResponse;
    private String customProperty;

    @Override
    public List<RestResourceConfig> getResources() {
        return resources;
    }

    @Override
    public boolean isDefaultsFromRootResponse() {
        return defaultsFromRootResponse;
    }

    public String getCustomProperty() {
        return customProperty;
    }
}
