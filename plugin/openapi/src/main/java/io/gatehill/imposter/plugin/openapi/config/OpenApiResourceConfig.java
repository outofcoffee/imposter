package io.gatehill.imposter.plugin.openapi.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.Map;

/**
 * Extends a REST resource configuration with more specific matching criteria.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiResourceConfig extends RestResourceConfig implements PathParamsResourceConfig, QueryParamsResourceConfig {
    @JsonProperty("pathParams")
    private Map<String, String> pathParams;

    @JsonProperty("queryParams")
    @JsonAlias("params")
    private Map<String, String> queryParams;

    @Override
    public Map<String, String> getPathParams() {
        return pathParams;
    }

    @Override
    public Map<String, String> getParams() {
        return getQueryParams();
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public OpenApiResponseConfig getResponseConfig() {
        return responseConfig;
    }

    @JsonProperty("response")
    private OpenApiResponseConfig responseConfig = new OpenApiResponseConfig();
}
