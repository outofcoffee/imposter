package io.gatehill.imposter.plugin.openapi.config;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginValidationConfig {
    private Boolean request;
    private Boolean response;
    private Boolean returnErrorsInResponse = true;
    private Map<String, String> levels;

    public Boolean getRequest() {
        return request;
    }

    public Boolean getResponse() {
        return response;
    }

    public Boolean getReturnErrorsInResponse() {
        return returnErrorsInResponse;
    }

    public Map<String, String> getLevels() {
        return levels;
    }
}
