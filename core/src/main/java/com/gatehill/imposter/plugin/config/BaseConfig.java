package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseConfig {
    @JsonProperty("plugin")
    private String pluginClass;

    @JsonProperty("basePath")
    private String basePath;

    @JsonProperty("response")
    private ResponseConfig responseConfig;

    public String getPluginClass() {
        return pluginClass;
    }

    public String getBasePath() {
        return basePath;
    }

    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }
}
