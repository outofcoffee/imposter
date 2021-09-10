package io.gatehill.imposter.plugin.config.system;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreConfig {
    @JsonProperty("preloadData")
    private Map<String, Object> preloadData;

    @JsonProperty("preloadFile")
    private String preloadFile;

    public Map<String, Object> getPreloadData() {
        return preloadData;
    }

    public String getPreloadFile() {
        return preloadFile;
    }
}
