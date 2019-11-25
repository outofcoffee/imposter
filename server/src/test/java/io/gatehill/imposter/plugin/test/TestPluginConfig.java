package io.gatehill.imposter.plugin.test;

import io.gatehill.imposter.plugin.config.PluginConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestPluginConfig extends PluginConfigImpl {
    private String customProperty;

    public String getCustomProperty() {
        return customProperty;
    }
}
