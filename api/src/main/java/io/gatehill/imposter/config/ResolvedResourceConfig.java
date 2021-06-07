package io.gatehill.imposter.config;

import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResolvedResourceConfig {
    private final RestResourceConfig config;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;

    public ResolvedResourceConfig(RestResourceConfig config, Map<String, String> pathParams, Map<String, String> queryParams) {
        this.config = config;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
    }

    public RestResourceConfig getConfig() {
        return config;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }
}
