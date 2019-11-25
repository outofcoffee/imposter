package io.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gatehill.imposter.plugin.config.resource.AbstractResourceConfig;

import java.io.File;

/**
 * Base configuration for plugins.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConfigImpl extends AbstractResourceConfig implements PluginConfig {
    private String plugin;

    @Override
    public String getPlugin() {
        return plugin;
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
