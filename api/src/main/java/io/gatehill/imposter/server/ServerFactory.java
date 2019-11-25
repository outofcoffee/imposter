package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ServerFactory {
    /**
     * Provides an {@link HttpServer}.
     * <p>
     * The method <strong>must</strong> complete the {@code startFuture} in order
     * for startup to complete successfully.
     *
     * @param imposterConfig the Imposter engine configuration
     * @param startFuture    the future on which the outcome should be signaled
     * @param vertx          the current Vert.x instance
     * @param router         the router
     * @return a server
     */
    HttpServer provide(ImposterConfig imposterConfig, Future<?> startFuture, Vertx vertx, Router router);
}
