package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseConfig {
    @JsonProperty("staticFile")
    private String staticFile;

    @JsonProperty("scriptFile")
    private String scriptFile;

    @JsonProperty("statusCode")
    private Integer statusCode;

    public String getStaticFile() {
        return staticFile;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
}
