package com.gatehill.imposter.server;

import com.gatehill.imposter.Imposter;
import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.server.util.ConfigUtil;
import com.gatehill.imposter.util.FileUtil;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.InjectorUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.HttpServerImpl;
import io.vertx.core.impl.FutureFactoryImpl;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.FutureFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.gatehill.imposter.util.AsyncUtil.resolveFutureOnCompletion;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

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

        FileWatcher fw = new FileWatcher();
        Thread t = new Thread(fw);
        t.start();

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
        ofNullable(httpServer).ifPresent(server -> server.close(resolveFutureOnCompletion(stopFuture)));
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

    class FileWatcher implements Runnable {

        private final Future<Void> startFuture;
        private final Future<Void> future;
        Map<String, WatchService> watchServices = new HashMap<>();

        FileWatcher() {
            this.startFuture = Future.future();
            this.future = new FutureFactoryImpl().future();

            Arrays.stream(imposterConfig.getConfigDirs()).forEachOrdered(config -> {
                Path path = Paths.get(config);

                try (Stream<Path> paths = Files.walk(path)) {

                    List<Path> yamls = paths.filter(Files::isRegularFile)
                            .filter(filePath -> {
                                try {
                                    return (filePath.toFile().getCanonicalPath().endsWith("yaml") ||
                                            filePath.toFile().getCanonicalPath().endsWith("yml"));
                                } catch (IOException io) {
                                    System.out.println(io.getLocalizedMessage());
                                    return false;
                                }
                            }).collect(toList());


                    yamls.forEach(p -> {
                        try{
                            String actualPath = p.toFile().getCanonicalPath();
                            watchServices.put(actualPath, Paths.get(actualPath).getParent().getFileSystem().newWatchService());
                        } catch(IOException io){
                            System.out.println(io.getLocalizedMessage());
                        }
                    });

                    watchServices.forEach((s,w)-> {
                        try {
                            Paths.get(s).getParent().register(w, StandardWatchEventKinds.ENTRY_MODIFY);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException io) {
                    System.out.println(io.getLocalizedMessage());
                }
            });

        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while(true) {
                watchServices.forEach((s,w) -> {
                    WatchKey watchKey;
                    try {
                        watchKey = w.take();
                        if (watchKey != null) {
                            watchKey.pollEvents().forEach(event -> System.out.println("File changed restarting server"));
                            synchronized (this.future) {
                                if(!this.future.isComplete()) {
                                    this.future.setHandler(result -> {
                                        if(result.succeeded()) {
                                            start(this.startFuture);
                                        }
                                    });
                                    httpServer.close(this.future.completer());
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                });
            }
        }
    }
}
