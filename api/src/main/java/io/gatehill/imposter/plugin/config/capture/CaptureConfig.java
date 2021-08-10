package io.gatehill.imposter.plugin.config.capture;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CaptureConfig {
    @JsonProperty("store")
    private String store;

    @JsonProperty("pathParam")
    private String pathParam;

    @JsonProperty("queryParam")
    private String queryParam;

    @JsonProperty("requestHeader")
    private String requestHeader;

    @JsonProperty("jsonPath")
    private String jsonPath;

    public String getStore() {
        return store;
    }

    public String getPathParam() {
        return pathParam;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public String getRequestHeader() {
        return requestHeader;
    }

    public String getJsonPath() {
        return jsonPath;
    }
}
