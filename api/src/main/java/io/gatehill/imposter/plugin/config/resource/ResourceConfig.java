package io.gatehill.imposter.plugin.config.resource;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResourceConfig {
    String getPath();

    ResponseConfig getResponseConfig();
}
