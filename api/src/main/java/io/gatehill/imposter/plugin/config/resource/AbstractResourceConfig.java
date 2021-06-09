package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractResourceConfig implements ResponseConfigHolder, SecurityConfigHolder {
    @JsonProperty("path")
    private String path;

    @JsonProperty("security")
    private SecurityConfig security;

    @SuppressWarnings("FieldMayBeFinal")
    @JsonProperty("response")
    private ResponseConfig responseConfig = new ResponseConfig();

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public SecurityConfig getSecurity() {
        return security;
    }

    @Override
    public ResponseConfig getResponseConfig() {
        return responseConfig;
    }
}
