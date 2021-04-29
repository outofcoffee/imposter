package io.gatehill.imposter.plugin.openapi.config;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginValidationConfig {
    private Boolean request;
    private Boolean response;

    public Boolean getRequest() {
        return request;
    }

    public Boolean getResponse() {
        return response;
    }
}
