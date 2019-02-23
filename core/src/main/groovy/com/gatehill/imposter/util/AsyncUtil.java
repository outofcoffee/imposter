package com.gatehill.imposter.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class AsyncUtil {
    private AsyncUtil() {
    }

    /**
     * Builds a {@link Handler} that processes a request asynchronously. Upon receiving a request,
     * the {@code routingContextConsumer} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleAsync(routingContext -> {
     *     // use routingContext
     * });
     * </pre>
     *
     * @param routingContextConsumer the consumer of the {@link RoutingContext}
     * @return the handler
     */
    public static Handler<RoutingContext> handleAsync(Consumer<RoutingContext> routingContextConsumer) {
        return routingContext -> Vertx.currentContext().executeBlocking(future -> {
            try {
                routingContextConsumer.accept(routingContext);
                future.complete();
            } catch (Exception e) {
                routingContext.fail(new RuntimeException(String.format("Unhandled exception processing %s request %s",
                        routingContext.request().method(), routingContext.request().absoluteURI()), e));
            }

        }, result -> { /* no op */ });
    }

    public static <T> Handler<AsyncResult<T>> resolveFutureOnCompletion(Future<?> future) {
        return completion -> {
            if (completion.succeeded()) {
                future.complete();
            } else {
                future.fail(completion.cause());
            }
        };
    }
}
