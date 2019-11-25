package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.FileUtil;
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

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class VertxWebServerFactoryImpl implements ServerFactory {
    private static final Logger LOGGER = LogManager.getLogger(VertxWebServerFactoryImpl.class);

    @Override
    public HttpServer provide(ImposterConfig imposterConfig, Future<?> startFuture, Vertx vertx, Router router) {
        LOGGER.info("Starting mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        final HttpServerOptions serverOptions = new HttpServerOptions();

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled()) {
            LOGGER.info("TLS is enabled");

            // locate keystore
            final Path keystorePath;
            if (imposterConfig.getKeystorePath().startsWith(FileUtil.CLASSPATH_PREFIX)) {
                try {
                    keystorePath = Paths.get(VertxWebServerFactoryImpl.class.getResource(
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
        return vertx.createHttpServer(serverOptions)
                .requestHandler(router::accept)
                .listen(imposterConfig.getListenPort(), imposterConfig.getHost(), AsyncUtil.resolveFutureOnCompletion(startFuture));
    }
}
