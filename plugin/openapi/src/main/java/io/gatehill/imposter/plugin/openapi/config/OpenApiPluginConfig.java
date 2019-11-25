package io.gatehill.imposter.plugin.openapi.config;

import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginConfig extends ContentTypedPluginConfigImpl {
    private String specFile;
    private boolean pickFirstIfNoneMatch = true;
    private boolean useServerPathAsBaseUrl = true;

    public String getSpecFile() {
        return specFile;
    }

    public boolean isPickFirstIfNoneMatch() {
        return pickFirstIfNoneMatch;
    }

    public boolean isUseServerPathAsBaseUrl() {
        return useServerPathAsBaseUrl;
    }
}
