package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ServerFactory {
    HttpServer provide(ImposterConfig imposterConfig, Future<Void> startFuture, Vertx vertx, Router router);
}
