package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@FunctionalInterface
public interface StatusCodeFactory {
    int calculateStatus(ResponseConfigHolder resourceConfig);
}
