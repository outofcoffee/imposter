package io.gatehill.imposter.plugin.config;

import java.io.File;
import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ConfigurablePlugin<C extends PluginConfig> {
    void loadConfiguration(List<File> configFiles);

    List<C> getConfigs();
}
