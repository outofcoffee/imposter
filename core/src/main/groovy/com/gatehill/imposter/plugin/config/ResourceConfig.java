package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResourceConfig {
    @JsonProperty("path")
    private String path;

    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();

    public String getPath() {
        return path;
    }

    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }
}
