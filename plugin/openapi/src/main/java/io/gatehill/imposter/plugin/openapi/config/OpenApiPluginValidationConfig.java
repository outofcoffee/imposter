package io.gatehill.imposter.plugin.openapi.config;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginValidationConfig {
    private Boolean request;
    private Boolean response;
    private Boolean returnErrorsInResponse = true;

    public Boolean getRequest() {
        return request;
    }

    public Boolean getResponse() {
        return response;
    }

    public Boolean getReturnErrorsInResponse() {
        return returnErrorsInResponse;
    }
}
