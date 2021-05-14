package io.gatehill.imposter.plugin.openapi.config;

import io.gatehill.imposter.plugin.config.resource.ParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.Map;

/**
 * Extends a REST resource configuration with more specific matching criteria.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginResourceConfig extends RestResourceConfig implements ParamsResourceConfig {
    private Map<String, String> params;

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
