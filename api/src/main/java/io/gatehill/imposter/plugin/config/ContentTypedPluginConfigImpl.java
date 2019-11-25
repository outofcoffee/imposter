package io.gatehill.imposter.plugin.config;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ContentTypedPluginConfigImpl extends PluginConfigImpl implements ContentTypedConfig {
    protected String contentType;

    @Override
    public String getContentType() {
        return contentType;
    }
}
