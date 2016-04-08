package com.gatehill.imposter.server;

import com.gatehill.imposter.Imposter;
import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.util.FileUtil;
import com.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
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

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    @Override
    public void start() {
        new Imposter().start();
        InjectorUtil.getInjector().injectMembers(this);
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

    private void startServer() {
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
