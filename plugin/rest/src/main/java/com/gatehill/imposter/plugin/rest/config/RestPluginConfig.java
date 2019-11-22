package com.gatehill.imposter.plugin.rest.config;

import com.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends ContentTypedPluginConfigImpl {
    private List<RestResourceConfig> resources;

    public List<RestResourceConfig> getResources() {
        return resources;
    }
}
