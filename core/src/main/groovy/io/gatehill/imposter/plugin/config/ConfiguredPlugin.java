package io.gatehill.imposter.plugin.config;

import com.google.inject.Injector;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.util.ConfigUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.Vertx;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class ConfiguredPlugin<T extends PluginConfigImpl> implements Plugin, ConfigurablePlugin<T> {
    @Inject
    protected Vertx vertx;

    private List<T> configs;

    protected abstract Class<T> getConfigClass();

    @Override
    public void loadConfiguration(List<File> configFiles) {
        configs = configFiles.stream()
                .map(file -> ConfigUtil.loadPluginConfig(file, getConfigClass(), true))
                .collect(Collectors.toList());

        this.configurePlugin(configs);
    }

    @Override
    public List<T> getConfigs() {
        return configs;
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
