package io.gatehill.imposter.plugin.rest.config;

import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

/**
 * Extends a REST resource configuration with a content type and resource type.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginResourceConfig extends RestResourceConfig implements ContentTypedConfig {
    protected String contentType;
    private ResourceConfigType type;

    @Override
    public String getContentType() {
        return contentType;
    }

    public ResourceConfigType getType() {
        return type;
    }
}
