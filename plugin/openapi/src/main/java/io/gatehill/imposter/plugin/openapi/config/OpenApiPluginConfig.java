package io.gatehill.imposter.plugin.openapi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.ContentTypedPluginConfigImpl;
import io.gatehill.imposter.plugin.config.ResourcesHolder;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginConfig extends ContentTypedPluginConfigImpl implements ResourcesHolder<OpenApiResourceConfig> {
    @JsonProperty("specFile")
    private String specFile;

    @JsonProperty("resources")
    private List<OpenApiResourceConfig> resources;

    @JsonProperty("pickFirstIfNoneMatch")
    private boolean pickFirstIfNoneMatch = true;

    @JsonProperty("useServerPathAsBaseUrl")
    private boolean useServerPathAsBaseUrl = true;

    @JsonProperty("response")
    private OpenApiResponseConfig responseConfig = new OpenApiResponseConfig();

    @JsonProperty("validation")
    private OpenApiPluginValidationConfig validation;

    public String getSpecFile() {
        return specFile;
    }

    @Override
    public List<OpenApiResourceConfig> getResources() {
        return resources;
    }

    public boolean isPickFirstIfNoneMatch() {
        return pickFirstIfNoneMatch;
    }

    public boolean isUseServerPathAsBaseUrl() {
        return useServerPathAsBaseUrl;
    }

    @Override
    public OpenApiResponseConfig getResponseConfig() {
        return responseConfig;
    }

    public OpenApiPluginValidationConfig getValidation() {
        return validation;
    }
}
