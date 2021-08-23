package io.gatehill.imposter.plugin.config;

import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;

import java.util.List;

/**
 * Represents a list of resource configurations.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResourcesHolder<T extends RestResourceConfig> {
    List<T> getResources();

    boolean isDefaultsFromRootResponse();
}
