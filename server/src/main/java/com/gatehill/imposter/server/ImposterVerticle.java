package com.gatehill.imposter.server;

import com.gatehill.imposter.Imposter;
import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

import static com.gatehill.imposter.util.AsyncUtil.resolveFutureOnCompletion;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ServerFactory serverFactory;

    private HttpServer httpServer;

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.debug("Initialising mock server");

        new Imposter().start();
        InjectorUtil.getInjector().injectMembers(this);
        startServer(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        ofNullable(httpServer).ifPresent(server -> server.close(resolveFutureOnCompletion(stopFuture)));
    }

    private void startServer(Future<Void> startFuture) {
        final Router router = configureRoutes();
        httpServer = serverFactory.provide(imposterConfig, startFuture, vertx, router);
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
