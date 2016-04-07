package com.gatehill.imposter.plugin.config;

import com.gatehill.imposter.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.MapUtil.MAPPER;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class ConfiguredPlugin<T extends BaseConfig> implements Plugin, ConfigurablePlugin {
    protected abstract Class<T> getConfigClass();

    @Override
    public void loadConfiguration(List<File> configFiles) {
        final List<T> configs = configFiles.stream()
                .map(file -> {
                    try {
                        return MAPPER.readValue(file, getConfigClass());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        this.configurePlugin(configs);
    }

    /**
     * Strongly typed configuration objects for this plugin.
     *
     * @param configs
     */
    protected abstract void configurePlugin(List<T> configs);
}
