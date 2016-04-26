package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseConfig extends ResourceConfig {
    @JsonProperty("plugin")
    private String pluginClass;

    public String getPluginClass() {
        return pluginClass;
    }
}
