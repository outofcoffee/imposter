package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.plugin.config.ContentTypedBaseConfig;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends ContentTypedBaseConfig {
    private List<RestResourceConfig> resources;

    public List<RestResourceConfig> getResources() {
        return resources;
    }
}
