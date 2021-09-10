package io.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.resource.AbstractResourceConfig;
import io.gatehill.imposter.plugin.config.system.SystemConfig;
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder;

import java.io.File;

/**
 * Base configuration for plugins.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConfigImpl extends AbstractResourceConfig implements SystemConfigHolder, PluginConfig {
    @JsonProperty("plugin")
    private String plugin;

    @JsonProperty("parentDir")
    private File parentDir;

    @JsonProperty("system")
    private SystemConfig systemConfig;

    @Override
    public String getPlugin() {
        return plugin;
    }

    public void setParentDir(File parentDir) {
        this.parentDir = parentDir;
    }

    @Override
    public File getParentDir() {
        return parentDir;
    }

    @Override
    public SystemConfig getSystemConfig() {
        return systemConfig;
    }
}
