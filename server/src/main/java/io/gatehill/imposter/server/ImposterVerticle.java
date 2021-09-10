package io.gatehill.imposter.server;

import com.google.inject.Module;
import io.gatehill.imposter.Imposter;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.ConfigurablePlugin;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.system.SystemConfig;
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder;
import io.gatehill.imposter.scripting.groovy.GroovyScriptingModule;
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule;
import io.gatehill.imposter.server.util.FeatureModuleUtil;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.FeatureUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import io.vertx.micrometer.PrometheusScrapingHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ServerFactory serverFactory;

    @Inject
    private ImposterLifecycleHooks lifecycleHooks;

    @Inject
    private ResourceService resourceService;

    private final ImposterConfig imposterConfig;

    private HttpServer httpServer;

    public ImposterVerticle() {
        imposterConfig = ConfigHolder.getConfig();
    }

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.debug("Initialising mock server");

        vertx.executeBlocking(future -> {
            try {
                final Imposter imposter = startEngine();
                InjectorUtil.getInjector().injectMembers(ImposterVerticle.this);

                final List<PluginConfig> allConfigs = parsePluginConfiguration(imposter);
                final Router routes = configureRoutes(allConfigs);
                httpServer = serverFactory.provide(imposterConfig, future, vertx, routes);
                LOGGER.info("Mock engine up and running");

            } catch (Exception e) {
                future.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                startFuture.fail(result.cause());
            } else {
                startFuture.complete();
            }
        });
    }

    private List<PluginConfig> parsePluginConfiguration(Imposter imposter) {
        final List<PluginConfig> allConfigs = new ArrayList<>();
        pluginManager.getPlugins().stream()
                .filter(p -> p instanceof ConfigurablePlugin)
                .forEach(p -> allConfigs.addAll(((ConfigurablePlugin<?>) p).getConfigs()));

        if (allConfigs.isEmpty()) {
            throw new IllegalStateException("No plugin configurations were found. The configuration directory must contain one or more valid Imposter configuration files compatible with installed plugins.");
        }

        finaliseListenPort(allConfigs);
        imposter.configureServerUrl();

        return allConfigs;
    }

    private Imposter startEngine() {
        final List<Module> bootstrapModules = newArrayList(
                new BootstrapModule(vertx, imposterConfig, imposterConfig.getServerFactory()),
                new GroovyScriptingModule(),
                new NashornScriptingModule()
        );
        bootstrapModules.addAll(FeatureModuleUtil.discoverFeatureModules());

        final Imposter imposter = new Imposter(imposterConfig, bootstrapModules);
        imposter.start();
        return imposter;
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        ofNullable(httpServer).ifPresent(server -> server.close(AsyncUtil.resolveFutureOnCompletion(stopFuture)));
    }

    private Router configureRoutes(List<PluginConfig> allConfigs) {
        final Router router = Router.router(vertx);
        router.errorHandler(500, resourceService.buildUnhandledExceptionHandler());
        router.route().handler(new BodyHandlerImpl());

        if (FeatureUtil.isFeatureEnabled("metrics")) {
            LOGGER.debug("Metrics enabled");

            router.route("/system/metrics").handler(
                    resourceService.passthroughRoute(imposterConfig, allConfigs, vertx, PrometheusScrapingHandler.create())
            );
        }

        // status check to indicate when server is up
        router.get("/system/status").handler(resourceService.handleRoute(imposterConfig, allConfigs, vertx, routingContext ->
                routingContext.response()
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                        .end(HttpUtil.buildStatusResponse())
        ));

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        // fire post route config hooks
        lifecycleHooks.forEach(listener -> listener.afterRoutesConfigured(imposterConfig, allConfigs, router));

        return router;
    }

    /**
     * Finalise the listen port.
     * Falls back to plugin provided configuration if not explicitly set in imposter config.
     *
     * @param allConfigs all plugin configurations
     */
    private void finaliseListenPort(List<PluginConfig> allConfigs) {
        if (!imposterConfig.getPortSetExplicitly()) {
            final List<Integer> pluginProvidedListenPorts = newArrayList();
            allConfigs.forEach(pluginConfig -> {
                if (pluginConfig instanceof SystemConfigHolder) {
                    final SystemConfig systemConfig = ((SystemConfigHolder) pluginConfig).getSystemConfig();
                    if (nonNull(systemConfig) && nonNull(systemConfig.getServerConfig()) && nonNull(systemConfig.getServerConfig().getListenPort())) {
                        pluginProvidedListenPorts.add(systemConfig.getServerConfig().getListenPort());
                    }
                }
            });

            switch (pluginProvidedListenPorts.size()) {
                case 0:
                    break;
                case 1:
                    final Integer pluginProvidedPort = pluginProvidedListenPorts.get(0);
                    imposterConfig.setListenPort(pluginProvidedPort);
                    LOGGER.trace("Set listen port to {} from plugin configuration", pluginProvidedPort);
                    break;
                default:
                    throw new IllegalStateException("Cannot specify 'system.server.port' configuration more than once. Ensure only one configuration file contains the root 'system.server.port' configuration.");
            }
        }
    }
}
