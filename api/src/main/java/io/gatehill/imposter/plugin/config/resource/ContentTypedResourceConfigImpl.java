package io.gatehill.imposter.plugin.config.resource;

import io.gatehill.imposter.plugin.config.ContentTypedConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ContentTypedResourceConfigImpl extends AbstractResourceConfig implements ContentTypedConfig {
    protected String contentType;

    @Override
    public String getContentType() {
        return contentType;
    }
}
