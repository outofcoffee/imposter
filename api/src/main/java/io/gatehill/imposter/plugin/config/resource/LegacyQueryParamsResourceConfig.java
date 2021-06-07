package io.gatehill.imposter.plugin.config.resource;

import java.util.Map;

/**
 * Deprecated and superseded by {@link QueryParamsResourceConfig#getQueryParams()}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface LegacyQueryParamsResourceConfig {
    /**
     * Use {@link QueryParamsResourceConfig#getQueryParams()} instead.
     */
    @Deprecated
    Map<String, String> getParams();
}
