package io.gatehill.imposter.plugin.config;

import io.gatehill.imposter.plugin.config.resource.ResourceConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ContentTypedConfig extends ResourceConfig {
    String getContentType();
}
