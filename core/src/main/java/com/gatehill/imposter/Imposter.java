package com.gatehill.imposter;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD;
import static com.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH;
import static com.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX;
import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_SUFFIX;
import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.gatehill.imposter.util.MapUtil.MAPPER;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);
    public static final String CONFIG_PREFIX = "com.gatehill.imposter.";

    @Inject
    private Injector injector;

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    public void start() {
        initDependencyInjection();
        configureSystem();
        configurePlugins();
    }

    @SuppressWarnings("unchecked")
    private void initDependencyInjection() {
        InjectorUtil.create(getModules()).injectMembers(this);

        ofNullable(System.getProperty(CONFIG_PREFIX + "plugin"))
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
            throw new RuntimeException(String.format(
                    "No plugins were loaded. Specify system property '%splugin'", CONFIG_PREFIX));
        }
    }

    private Module getModules() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(ImposterConfig.class).in(Singleton.class);
                bind(PluginManager.class).in(Singleton.class);
            }
        };
    }

    private void configureSystem() {
        imposterConfig.setListenPort(ofNullable(System.getProperty(CONFIG_PREFIX + "listenPort"))
                .map(Integer::parseInt)
                .orElse(8443));

        imposterConfig.setHost(System.getProperty(CONFIG_PREFIX + "host", BIND_ALL_HOSTS));

        imposterConfig.setTlsEnabled(Boolean.parseBoolean(System.getProperty(CONFIG_PREFIX + "tls", "false")));

        imposterConfig.setKeystorePath(System.getProperty(CONFIG_PREFIX + "keystorePath",
                CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH));

        imposterConfig.setKeystorePassword(System.getProperty(CONFIG_PREFIX + "keystorePassword",
                DEFAULT_KEYSTORE_PASSWORD));

        imposterConfig.setServerUrl(buildServerUrl());

        imposterConfig.setConfigDir(ofNullable(System.getProperty(CONFIG_PREFIX + "configDir"))
                .map(cps -> (cps.startsWith(".") ? System.getProperty("user.dir") + cps.substring(1) : cps))
                .orElseThrow(() -> new RuntimeException(String.format(
                        "System property '%sconfigDir' must be set to a directory", CONFIG_PREFIX))));
    }

    private URI buildServerUrl() {
        // might be set explicitly
        final Optional<String> explicitUrl = ofNullable(System.getProperty(CONFIG_PREFIX + "serverUrl"));
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

    private void configurePlugins() {
        // read all config files
        final Map<String, List<File>> mockConfigs = Maps.newHashMap();
        try {
            final File configDir = new File(imposterConfig.getConfigDir());
            final File[] configFiles = ofNullable(configDir.listFiles((dir, name) -> name.endsWith(CONFIG_FILE_SUFFIX)))
                    .orElse(new File[0]);

            for (File configFile : configFiles) {
                final BaseConfig config = MAPPER.readValue(configFile, BaseConfig.class);

                List<File> pluginConfigs = mockConfigs.get(config.getPlugin());
                if (null == pluginConfigs) {
                    pluginConfigs = Lists.newArrayList();
                    mockConfigs.put(config.getPlugin(), pluginConfigs);
                }

                pluginConfigs.add(configFile);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Loaded {} mock configs from: {}", mockConfigs.size(), imposterConfig.getConfigDir());

        // send config to plugins
        pluginManager.getPlugins().stream()
                .filter(plugin -> ConfigurablePlugin.class.isAssignableFrom(plugin.getClass()))
                .forEach(plugin -> {
                    final List<File> configFiles = mockConfigs.get(plugin.getClass().getCanonicalName());
                    ((ConfigurablePlugin) plugin).loadConfiguration(configFiles);
                });
    }
}
