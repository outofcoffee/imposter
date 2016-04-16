package com.gatehill.imposter.server;

import com.gatehill.imposter.Imposter;
import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.util.FileUtil;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private HttpServer httpServer;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
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

    private <T> Handler<AsyncResult<T>> resolveFutureOnCompletion(Future<Void> future) {
        return completion -> {
            if (completion.succeeded()) {
                future.complete();
            } else {
                future.fail(completion.cause());
            }
        };
    }

    private void startServer(Future<Void> startFuture) {
        final Router router = configureRoutes();

        LOGGER.info("Starting mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        final HttpServerOptions serverOptions = new HttpServerOptions();

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled()) {
            LOGGER.info("TLS is enabled");

            // locate keystore
            final Path keystorePath;
            if (imposterConfig.getKeystorePath().startsWith(FileUtil.CLASSPATH_PREFIX)) {
                try {
                    keystorePath = Paths.get(ImposterVerticle.class.getResource(
                            imposterConfig.getKeystorePath().substring(FileUtil.CLASSPATH_PREFIX.length())).toURI());
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Error locating keystore", e);
                }
            } else {
                keystorePath = Paths.get(imposterConfig.getKeystorePath());
            }

            final JksOptions jksOptions = new JksOptions();
            jksOptions.setPath(keystorePath.toString());
            jksOptions.setPassword(imposterConfig.getKeystorePassword());
            serverOptions.setKeyStoreOptions(jksOptions);
            serverOptions.setSsl(true);

        } else {
            LOGGER.info("TLS is disabled");
        }

        LOGGER.info("Listening on {}", imposterConfig.getServerUrl());

        httpServer = vertx.createHttpServer(serverOptions)
                .requestHandler(router::accept)
                .listen(imposterConfig.getListenPort(), imposterConfig.getHost(), resolveFutureOnCompletion(startFuture));
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);

        router.route().handler(new BodyHandlerImpl());

        // status check to indicate when server is up
        router.get("/system/status").handler(routingContext -> routingContext.response()
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(HttpUtil.STATUS_RESPONSE));

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        return router;
    }
}
