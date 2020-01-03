package com.gatehill.imposter.plugin.config;

import com.gatehill.imposter.plugin.config.resource.ResourceConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ContentTypedConfig extends ResourceConfig {
    String getContentType();
}
