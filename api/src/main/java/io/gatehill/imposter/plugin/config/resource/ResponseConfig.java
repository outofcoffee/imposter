package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseConfig {
    @JsonProperty("staticFile")
    private String staticFile;

    @JsonProperty("staticData")
    private String staticData;

    @JsonProperty("scriptFile")
    private String scriptFile;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("headers")
    private Map<String, String> headers;

    public String getStaticFile() {
        return staticFile;
    }

    public String getStaticData() {
        return staticData;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
