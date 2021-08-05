package io.gatehill.imposter.plugin.config.resource;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface RequestHeadersResourceConfig {
    Map<String, String> getRequestHeaders();
}
