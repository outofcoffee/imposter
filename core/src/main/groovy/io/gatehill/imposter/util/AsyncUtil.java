package io.gatehill.imposter.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class AsyncUtil {
    private AsyncUtil() {
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
