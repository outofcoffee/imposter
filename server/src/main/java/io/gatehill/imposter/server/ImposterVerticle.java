package io.gatehill.imposter.server;

import io.gatehill.imposter.Imposter;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.server.util.ConfigUtil;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

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

    private final ImposterConfig imposterConfig;

    private HttpServer httpServer;

    public ImposterVerticle() {
        imposterConfig = ConfigUtil.getConfig();
    }

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.debug("Initialising mock server");

        vertx.executeBlocking(future -> {
            try {
                startEngine();
                InjectorUtil.getInjector().injectMembers(ImposterVerticle.this);
                httpServer = serverFactory.provide(imposterConfig, future, vertx, configureRoutes());

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
        final BootstrapModule bootstrapModule = new BootstrapModule(vertx, imposterConfig.getServerFactory());
        final Imposter imposter = new Imposter(imposterConfig, bootstrapModule);
        imposter.start();
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        ofNullable(httpServer).ifPresent(server -> server.close(AsyncUtil.resolveFutureOnCompletion(stopFuture)));
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);

        router.route().handler(new BodyHandlerImpl());

        // status check to indicate when server is up
        router.get("/system/status").handler(routingContext -> routingContext.response()
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(HttpUtil.buildStatusResponse()));

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        return router;
    }
}
