package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractResourceConfig implements ResourceConfig {
    @JsonProperty("path")
    private String path;

    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }
}
