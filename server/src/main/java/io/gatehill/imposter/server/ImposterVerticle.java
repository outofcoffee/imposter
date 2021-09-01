package io.gatehill.imposter.server;

import com.google.inject.Module;
import io.gatehill.imposter.Imposter;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.ConfigurablePlugin;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.scripting.groovy.GroovyScriptingModule;
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule;
import io.gatehill.imposter.server.util.FeatureModuleUtil;
import io.gatehill.imposter.service.ResourceService;
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

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ServerManager serverManager;

    @Inject
    private ImposterLifecycleHooks lifecycleHooks;

    @Inject
    private ResourceService resourceService;

    private final ImposterConfig imposterConfig;

    private List<Future<HttpServer>> serverFutures;

    public ImposterVerticle() {
        imposterConfig = ConfigHolder.getConfig();
    }

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.debug("Initialising mock server");

        vertx.executeBlocking(future -> {
            try {
                startEngine();
                InjectorUtil.getInjector().injectMembers(ImposterVerticle.this);
                serverFutures = serverManager.provide(imposterConfig, future, vertx, configureRoutes());
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

    private void startEngine() {
        final List<Module> bootstrapModules = newArrayList(
                new BootstrapModule(vertx, imposterConfig, imposterConfig.getServerFactory()),
                new GroovyScriptingModule(),
                new NashornScriptingModule()
        );
        bootstrapModules.addAll(FeatureModuleUtil.discoverFeatureModules());

        final Imposter imposter = new Imposter(imposterConfig, bootstrapModules);
        imposter.start();
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        serverManager.stop(serverFutures, stopFuture);
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);
        router.errorHandler(500, resourceService.buildUnhandledExceptionHandler());
        router.route().handler(new BodyHandlerImpl());

        final List<PluginConfig> allConfigs = new ArrayList<>();
        pluginManager.getPlugins().stream()
                .filter(p -> p instanceof ConfigurablePlugin)
                .forEach(p -> allConfigs.addAll(((ConfigurablePlugin<?>) p).getConfigs()));

        if (allConfigs.isEmpty()) {
            throw new IllegalStateException("No plugin configurations were found. The configuration directory must contain one or more valid Imposter configuration files compatible with installed plugins.");
        }

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
}
