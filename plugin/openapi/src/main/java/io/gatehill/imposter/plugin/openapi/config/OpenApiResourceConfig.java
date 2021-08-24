package io.gatehill.imposter.plugin.openapi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

/**
 * Extends a REST resource configuration with more specific matching criteria.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiResourceConfig extends RestResourceConfig {

    @Override
    public OpenApiResponseConfig getResponseConfig() {
        return responseConfig;
    }

    @JsonProperty("response")
    private OpenApiResponseConfig responseConfig = new OpenApiResponseConfig();
}
