package io.gatehill.imposter.plugin.config.system;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ServerConfig {
    @JsonProperty("port")
    private Integer listenPort;

    public Integer getListenPort() {
        return listenPort;
    }
}
