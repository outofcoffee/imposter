package io.gatehill.imposter.plugin.openapi.config;

import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;
import io.gatehill.imposter.plugin.config.DefaultResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.ResourceResponseConfig;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginConfig extends ContentTypedPluginConfigImpl implements DefaultResourcesHolder {
    private String specFile;
    private List<ResourceResponseConfig> defaults;
    private boolean pickFirstIfNoneMatch = true;
    private boolean useServerPathAsBaseUrl = true;

    public String getSpecFile() {
        return specFile;
    }

    @Override
    public List<ResourceResponseConfig> getDefaults() {
        return defaults;
    }

    public boolean isPickFirstIfNoneMatch() {
        return pickFirstIfNoneMatch;
    }

    public boolean isUseServerPathAsBaseUrl() {
        return useServerPathAsBaseUrl;
    }
}
