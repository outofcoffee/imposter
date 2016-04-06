package com.gatehill.imposter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author pcornish
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ImposterConfig imposterConfig;

    private Injector injector;

    @Override
    public void start() {
        configureDependencyInjection();

        imposterConfig.setListenPort(ofNullable(System.getProperty("listenPort"))
                .map(Integer::parseInt)
                .orElse(8443));

        imposterConfig.setHost(ofNullable(System.getProperty("host"))
                .filter(h -> !h.isEmpty())
                .orElse("0.0.0.0"));

        imposterConfig.setConfigDir(ofNullable(System.getProperty("configDir"))
                .map(cps -> (cps.startsWith(".") ? System.getProperty("user.dir") + cps.substring(1) : cps))
                .orElseThrow(() -> new RuntimeException("System property 'configDir' must be set to a directory")));

        LOGGER.info("Starting mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());

        configureMocks();

        final Router router = configureRoutes();
        final HttpServerOptions serverOptions = new HttpServerOptions();

        // TODO: configure this + keystore and update test config to enable HTTPS
        serverOptions.setSsl(false);

        vertx.createHttpServer(serverOptions)
                .requestHandler(router::accept)
                .listen(imposterConfig.getListenPort(), imposterConfig.getHost());
    }

    private void configureMocks() {
        final ObjectMapper mapper = new ObjectMapper();

        final Map<String, BaseMockConfig> mockConfigs = Maps.newHashMap();
        try {
            final File configDir = new File(imposterConfig.getConfigDir());
            final File[] configFiles = ofNullable(configDir.listFiles((dir, name) -> name.endsWith("-config.json"))).orElse(new File[0]);

            for (File configFile : configFiles) {
                final BaseMockConfig config = mapper.readValue(configFile, BaseMockConfig.class);
                mockConfigs.put(config.getViewName(), config);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Loaded {} mock configs from: {}", mockConfigs.size(), imposterConfig.getConfigDir());
        pluginManager.getPlugins().forEach(plugin -> plugin.configureMocks(mockConfigs));
    }

    @Override
    public void stop() {
        if (null != vertx) {
            LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
            vertx.close();
            vertx = null;
        }
    }

    private void configureDependencyInjection() {
        injector = Guice.createInjector(getModules());
        injector.injectMembers(this);

        ofNullable(System.getProperty("pluginClass"))
                .ifPresent(clazz -> {
                    try {
                        pluginManager.registerClass((Class<? extends Plugin>) Class.forName(clazz));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });

        pluginManager.getPluginClasses()
                .forEach(pluginClass -> pluginManager.registerInstance(injector.getInstance(pluginClass)));

        LOGGER.info("Started {} plugins", pluginManager.getPluginClasses().size());
    }

    private Module getModules() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(PluginManager.class);
            }
        };
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);

        router.route().handler(new BodyHandlerImpl());

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        return router;
    }
}
