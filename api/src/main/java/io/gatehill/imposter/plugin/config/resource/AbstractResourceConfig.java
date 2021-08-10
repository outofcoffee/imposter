package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.capture.CaptureConfig;
import io.gatehill.imposter.plugin.config.capture.CaptureConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractResourceConfig implements ResponseConfigHolder, SecurityConfigHolder, CaptureConfigHolder {
    @JsonProperty("path")
    private String path;

    @JsonProperty("security")
    private SecurityConfig security;

    @JsonProperty("capture")
    private Map<String, CaptureConfig> capture;

    @SuppressWarnings("FieldMayBeFinal")
    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public SecurityConfig getSecurityConfig() {
        return security;
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
