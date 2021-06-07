package io.gatehill.imposter.plugin.config.resource;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface QueryParamsResourceConfig {
    /**
     * Use {@link #getQueryParams()} instead.
     */
    @Deprecated
    Map<String, String> getParams();

    Map<String, String> getQueryParams();
}
