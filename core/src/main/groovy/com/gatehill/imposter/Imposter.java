package com.gatehill.imposter;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_SUFFIX;
import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.gatehill.imposter.util.MapUtil.MAPPER;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);

    @Inject
    private Injector injector;

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    public void start() {
        InjectorUtil.createChildInjector(getModules()).injectMembers(this);

        processConfiguration();
        instantiatePlugins();
        configurePlugins();
    }

    protected Module[] getModules() {
        return new ImposterModule[]{new ImposterModule()};
    }

    private void processConfiguration() {
        imposterConfig.setServerUrl(buildServerUrl().toString());

        imposterConfig.setConfigDir(imposterConfig.getConfigDir().startsWith("./") ?
                Paths.get(System.getProperty("user.dir"), imposterConfig.getConfigDir().substring(2)).toString() :
                imposterConfig.getConfigDir());
    }

    private URI buildServerUrl() {
        // might be set explicitly
        final Optional<String> explicitUrl = ofNullable(imposterConfig.getServerUrl());
        if (explicitUrl.isPresent()) {
            return URI.create(explicitUrl.get());
        }

        // build based on configuration
        final String scheme = (imposterConfig.isTlsEnabled() ? "https" : "http") + "://";
        final String host = (BIND_ALL_HOSTS.equals(imposterConfig.getHost()) ? "localhost" : imposterConfig.getHost());

        final String port;
        if ((imposterConfig.isTlsEnabled() && 443 == imposterConfig.getListenPort())
                || (!imposterConfig.isTlsEnabled() && 80 == imposterConfig.getListenPort())) {
            port = "";
        } else {
            port = ":" + String.valueOf(imposterConfig.getListenPort());
        }

        return URI.create(scheme + host + port);
    }

    @SuppressWarnings("unchecked")
    private void instantiatePlugins() {
        ofNullable(imposterConfig.getPluginClassName())
                .ifPresent(clazz -> {
                    try {
                        pluginManager.registerClass((Class<? extends Plugin>) Class.forName(clazz));
                        LOGGER.debug("Registered plugin {}", clazz);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });

        pluginManager.getPluginClasses()
                .forEach(pluginClass -> pluginManager.registerInstance(injector.getInstance(pluginClass)));

        final int pluginCount = pluginManager.getPlugins().size();
        if (pluginCount > 0) {
            LOGGER.info("Started {} plugins", pluginCount);
        } else {
            throw new RuntimeException("No plugins were loaded");
        }
    }

    private void configurePlugins() {
        int configCount = 0;

        // read all config files
        final Map<String, List<File>> mockConfigs = Maps.newHashMap();
        try {
            final File configDir = new File(imposterConfig.getConfigDir());
            final File[] configFiles = ofNullable(configDir.listFiles((dir, name) -> name.endsWith(CONFIG_FILE_SUFFIX)))
                    .orElse(new File[0]);

            for (File configFile : configFiles) {
                LOGGER.debug("Loading configuration file: {}", configFile);
                configCount++;

                final BaseConfig config = MAPPER.readValue(configFile, BaseConfig.class);

                List<File> pluginConfigs = mockConfigs.get(config.getPluginClass());
                if (null == pluginConfigs) {
                    pluginConfigs = Lists.newArrayList();
                    mockConfigs.put(config.getPluginClass(), pluginConfigs);
                }

                pluginConfigs.add(configFile);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Loaded {} mock configuration files from: {}", configCount, imposterConfig.getConfigDir());

        // send config to plugins
        pluginManager.getPlugins().stream()
                .filter(plugin -> ConfigurablePlugin.class.isAssignableFrom(plugin.getClass()))
                .forEach(plugin -> {
                    final List<File> configFiles = mockConfigs.get(plugin.getClass().getCanonicalName());
                    ((ConfigurablePlugin) plugin).loadConfiguration(configFiles);
                });
    }
}
