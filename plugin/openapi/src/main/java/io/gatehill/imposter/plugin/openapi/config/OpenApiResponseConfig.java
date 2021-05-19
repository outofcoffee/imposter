package io.gatehill.imposter.plugin.openapi.config;

import io.gatehill.imposter.plugin.config.resource.ResponseConfig;

/**
 * Extends the base response configuration with configuration specific
 * to the OpenAPI plugin.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiResponseConfig extends ResponseConfig {
    private String exampleName;

    public String getExampleName() {
        return exampleName;
    }
}
