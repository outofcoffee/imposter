package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ServerManager {
    /**
     * Provides one or more {@link HttpServer}s.
     * <p>
     * The method <strong>must</strong> complete the {@code startFuture} in order
     * for startup to complete successfully.
     *
     * @param imposterConfig the Imposter engine configuration
     * @param startFuture    the future on which the outcome must be signaled
     * @param vertx          the current Vert.x instance
     * @param router         the router
     * @return the servers
     */
    List<Future<HttpServer>> provide(ImposterConfig imposterConfig, Future<?> startFuture, Vertx vertx, Router router);

    /**
     * Stop the servers provided by the given futures.
     * <p>
     * The method <strong>must</strong> complete the {@code stopFuture} in order
     * for shutdown to complete successfully.
     *
     * @param serverFutures the server futures
     * @param stopFuture    the future on which the outcome must be signaled
     */
    void stop(List<Future<HttpServer>> serverFutures, Future<?> stopFuture);
}
