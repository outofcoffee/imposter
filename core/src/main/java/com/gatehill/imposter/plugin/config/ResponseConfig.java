package com.gatehill.imposter.plugin.config;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseConfig {
    private String staticFile;
    private String scriptFile;
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
