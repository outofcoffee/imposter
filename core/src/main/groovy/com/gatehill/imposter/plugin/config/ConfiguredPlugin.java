package com.gatehill.imposter.plugin.config;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.inject.Injector;
import io.vertx.core.Vertx;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.MapUtil.MAPPER;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class ConfiguredPlugin<T extends BaseConfig> implements Plugin, ConfigurablePlugin {
    @Inject
    protected Vertx vertx;

    protected abstract Class<T> getConfigClass();

    @Override
    public void loadConfiguration(List<File> configFiles) {
        final List<T> configs = configFiles.stream()
                .map(file -> {
                    try {
                        final T config = MAPPER.readValue(file, getConfigClass());
                        config.setParentDir(file.getParentFile());
                        return config;

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

    protected Injector getInjector() {
        return InjectorUtil.getInjector();
    }
}
