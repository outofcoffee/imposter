package io.gatehill.imposter.plugin.test;

import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestPluginResourceConfig extends RestResourceConfig implements ContentTypedConfig {
    private String contentType;

    @Override
    public String getContentType() {
        return contentType;
    }
}
