package io.gatehill.imposter.plugin.config;

import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ContentTypedConfig extends ResponseConfigHolder {
    String getContentType();
}
