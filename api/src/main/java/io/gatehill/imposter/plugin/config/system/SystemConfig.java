package io.gatehill.imposter.plugin.config.system;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SystemConfig {
    @JsonProperty("stores")
    private Map<String, StoreConfig> storeConfigs;

    public Map<String, StoreConfig> getStoreConfigs() {
        return storeConfigs;
    }
}
