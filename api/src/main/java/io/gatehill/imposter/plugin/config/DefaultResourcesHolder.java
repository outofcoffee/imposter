package io.gatehill.imposter.plugin.config;

import io.gatehill.imposter.plugin.config.resource.ResourceResponseConfig;

import java.util.List;

/**
 * Represents a list of default resource response configurations.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface DefaultResourcesHolder {
    List<ResourceResponseConfig> getDefaults();
}
