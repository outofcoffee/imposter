package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.plugin.config.BaseConfig;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginConfig extends BaseConfig {
    private List<RestResourceConfig> resources;
    protected String contentType;

    public List<RestResourceConfig> getResources() {
        return resources;
    }

    public String getContentType() {
        return contentType;
    }
}
