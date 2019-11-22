package com.gatehill.imposter.plugin.config;

import java.io.File;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface PluginConfig {
    String getPluginClass();

    File getParentDir();
}
