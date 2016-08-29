package com.gatehill.imposter.plugin.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResourceConfig {
    @JsonProperty("path")
    private String path;

    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();
    private File parentDir;

    public String getPath() {
        return path;
    }

    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }

    public void setParentDir(File parentDir) {
        this.parentDir = parentDir;
    }

    public File getParentDir() {
        return parentDir;
    }
}
