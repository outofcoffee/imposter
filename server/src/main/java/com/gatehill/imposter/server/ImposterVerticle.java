package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.gatehill.imposter.util.MapUtil.MAPPER;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);
    public static final String CONFIG_PREFIX = "com.gatehill.imposter.";

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    private Injector injector;

    @Override
    public void start() {
        initDependencyInjection();
        configureSystem();
        configurePlugins();
        startServer();
    }

    @Override
    public void stop() {
        if (null != vertx) {
            LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
            vertx.close();
            vertx = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void initDependencyInjection() {
        injector = Guice.createInjector(getModules());
        injector.injectMembers(this);

        ofNullable(System.getProperty(CONFIG_PREFIX + "pluginClass"))
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
                    "No plugins were loaded. Specify system property '%spluginClass'", CONFIG_PREFIX));
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

        imposterConfig.setHost(ofNullable(System.getProperty(CONFIG_PREFIX + "host"))
                .filter(h -> !h.isEmpty())
                .orElse("0.0.0.0"));

        imposterConfig.setConfigDir(ofNullable(System.getProperty(CONFIG_PREFIX + "configDir"))
                .map(cps -> (cps.startsWith(".") ? System.getProperty("user.dir") + cps.substring(1) : cps))
                .orElseThrow(() -> new RuntimeException(String.format(
                        "System property '%sconfigDir' must be set to a directory", CONFIG_PREFIX))));
    }

    private void configurePlugins() {
        // read all config files
        final Map<String, List<File>> mockConfigs = Maps.newHashMap();
        try {
            final File configDir = new File(imposterConfig.getConfigDir());
            final File[] configFiles = ofNullable(configDir.listFiles((dir, name) -> name.endsWith("-config.json"))).orElse(new File[0]);

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
                .forEach(plugin -> ((ConfigurablePlugin) plugin).loadConfiguration(mockConfigs.get(plugin.getClass().getCanonicalName())));
    }

    private void startServer() {
        final Router router = configureRoutes();

        LOGGER.info("Starting mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        final HttpServerOptions serverOptions = new HttpServerOptions();

        // TODO: configure this + keystore and update test config to enable HTTPS
        serverOptions.setSsl(false);

        vertx.createHttpServer(serverOptions)
                .requestHandler(router::accept)
                .listen(imposterConfig.getListenPort(), imposterConfig.getHost());
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);

        router.route().handler(new BodyHandlerImpl());

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        return router;
    }
}
