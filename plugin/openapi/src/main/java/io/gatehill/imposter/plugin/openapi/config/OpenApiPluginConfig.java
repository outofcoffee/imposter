package io.gatehill.imposter.plugin.openapi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginConfig extends ContentTypedPluginConfigImpl implements ResourcesHolder<RestResourceConfig> {
    @JsonProperty("specFile")
    private String specFile;

    @JsonProperty("resources")
    private List<RestResourceConfig> resources;

    @JsonProperty("pickFirstIfNoneMatch")
    private boolean pickFirstIfNoneMatch = true;

    @JsonProperty("useServerPathAsBaseUrl")
    private boolean useServerPathAsBaseUrl = true;

    public String getSpecFile() {
        return specFile;
    }

    @Override
    public List<RestResourceConfig> getResources() {
        return resources;
    }

    public boolean isPickFirstIfNoneMatch() {
        return pickFirstIfNoneMatch;
    }

    public boolean isUseServerPathAsBaseUrl() {
        return useServerPathAsBaseUrl;
    }
}
