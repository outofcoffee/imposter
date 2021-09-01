package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.FileUtil;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class VertxWebServerManagerImpl implements ServerManager {
    private static final Logger LOGGER = LogManager.getLogger(VertxWebServerManagerImpl.class);
    private static final String ENV_SERVER_INSTANCES = "IMPOSTER_SERVER_INSTANCES";
    private static final int DEFAULT_SERVER_INSTANCES = 1;

    @Override
    public List<Future<HttpServer>> provide(ImposterConfig imposterConfig, Future<?> startFuture, Vertx vertx, Router router) {
        LOGGER.info("Starting mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        final HttpServerOptions serverOptions = new HttpServerOptions();

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled()) {
            LOGGER.info("TLS is enabled");
            configureTls(imposterConfig, serverOptions);
        } else {
            LOGGER.info("TLS is disabled");
        }

        final Integer serverInstances = ofNullable(System.getenv(ENV_SERVER_INSTANCES)).map(Integer::parseInt).orElse(DEFAULT_SERVER_INSTANCES);
        LOGGER.info("Starting {} server instance(s) listening on {}", serverInstances, imposterConfig.getServerUrl());

        final List<Future<HttpServer>> serverFutures = IntStream.of(serverInstances)
                .mapToObj(i -> createServer(imposterConfig, vertx, router, serverOptions))
                .collect(Collectors.toList());

        CompositeFuture.all(newArrayList(serverFutures)).setHandler(complete -> {
            if (complete.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(complete.cause());
            }
        });

        return serverFutures;
    }

    private void configureTls(ImposterConfig imposterConfig, HttpServerOptions serverOptions) {
        // locate keystore
        final Path keystorePath;
        if (imposterConfig.getKeystorePath().startsWith(FileUtil.CLASSPATH_PREFIX)) {
            try {
                keystorePath = Paths.get(VertxWebServerManagerImpl.class.getResource(
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
    }

    @Override
    public void stop(List<Future<HttpServer>> serverFutures, Future<?> stopFuture) {
        final List<Future<Void>> stopFutures = serverFutures.stream()
                .map(Future::result)
                .filter(Objects::nonNull)
                .map(this::stopServer)
                .collect(Collectors.toList());

        CompositeFuture.all(newArrayList(stopFutures)).setHandler(complete -> {
            if (complete.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(complete.cause());
            }
        });
    }

    private Future<HttpServer> createServer(ImposterConfig imposterConfig, Vertx vertx, Router router, HttpServerOptions serverOptions) {
        final Future<HttpServer> future = Future.future();
        final HttpServer server = vertx.createHttpServer(serverOptions).requestHandler(router);
        server.listen(imposterConfig.getListenPort(), imposterConfig.getHost(), AsyncUtil.resolveFutureOnCompletion(future, server));
        return future;
    }

    private Future<Void> stopServer(HttpServer server) {
        final Future<Void> future = Future.future();
        server.close(AsyncUtil.resolveFutureOnCompletion(future));
        return future;
    }
}
