package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatehill.imposter.plugin.config.resource.AbstractResourceConfig;

import java.io.File;

/**
 * Base configuration for plugins.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConfigImpl extends AbstractResourceConfig implements PluginConfig {
    @JsonProperty("plugin")
    private String pluginClass;

    @Override
    public String getPluginClass() {
        return pluginClass;
    }

    private File parentDir;

    public void setParentDir(File parentDir) {
        this.parentDir = parentDir;
    }

    @Override
    public File getParentDir() {
        return parentDir;
    }
}
