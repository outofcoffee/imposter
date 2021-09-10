package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.capture.CaptureConfig;
import io.gatehill.imposter.plugin.config.capture.CaptureConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;

import java.util.Map;

/**
 * Base configuration for plugins and sub-resources.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractResourceConfig implements ResponseConfigHolder, SecurityConfigHolder, CaptureConfigHolder {
    @JsonProperty("path")
    private String path;

    @JsonProperty("security")
    private SecurityConfig securityConfig;

    @JsonProperty("capture")
    private Map<String, CaptureConfig> capture;

    @SuppressWarnings("FieldMayBeFinal")
    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    @Override
    public Map<String, CaptureConfig> getCaptureConfig() {
        return capture;
    }

    @Override
    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }
}
