package io.gatehill.imposter.plugin.config;

import com.google.inject.Injector;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.util.ConfigUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.Vertx;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class ConfiguredPlugin<T extends PluginConfigImpl> implements Plugin, ConfigurablePlugin {
    @Inject
    protected Vertx vertx;

    protected abstract Class<T> getConfigClass();

    @Override
    public void loadConfiguration(List<File> configFiles) {
        final List<T> configs = configFiles.stream()
                .map(file -> {
                    try {
                        final T config = ConfigUtil.lookupMapper(file).readValue(file, getConfigClass());
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
