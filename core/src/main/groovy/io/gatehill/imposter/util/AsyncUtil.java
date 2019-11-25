package io.gatehill.imposter.util;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.server.RequestHandlingMode;
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
     * Builds a {@link Handler} that processes a request.
     * <p>
     * If {@code requestHandlingMode} is {@link RequestHandlingMode#SYNC}, then the {@code routingContextConsumer}
     * is invoked on the calling thread.
     * <p>
     * If it is {@link RequestHandlingMode#ASYNC}, then upon receiving a request,
     * the {@code routingContextConsumer} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleRoute(imposterConfig, vertx, routingContext -> {
     *     // use routingContext
     * });
     * </pre>
     *
     * @param vertx                  the current Vert.x instance
     * @param routingContextConsumer the consumer of the {@link RoutingContext}
     * @return the handler
     */
    public static Handler<RoutingContext> handleRoute(ImposterConfig imposterConfig, Vertx vertx,
                                                      Consumer<RoutingContext> routingContextConsumer) {

        switch (imposterConfig.getRequestHandlingMode()) {
            case SYNC:
                return routingContext -> {
                    try {
                        routingContextConsumer.accept(routingContext);
                    } catch (Exception e) {
                        handleFailure(routingContext, e);
                    }
                };

            case ASYNC:
                return routingContext -> vertx.getOrCreateContext().executeBlocking(future -> {
                    try {
                        routingContextConsumer.accept(routingContext);
                        future.complete();
                    } catch (Exception e) {
                        future.fail(e);
                    }

                }, result -> {
                    if (result.failed()) {
                        handleFailure(routingContext, result.cause());
                    }
                });

            default:
                throw new UnsupportedOperationException("Unsupported request handling mode: " + imposterConfig.getRequestHandlingMode());
        }
    }

    private static void handleFailure(RoutingContext routingContext, Throwable e) {
        routingContext.fail(new RuntimeException(String.format("Unhandled exception processing %s request %s",
                routingContext.request().method(), routingContext.request().absoluteURI()), e));
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
