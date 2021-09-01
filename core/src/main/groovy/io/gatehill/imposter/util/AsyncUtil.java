package io.gatehill.imposter.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class AsyncUtil {
    private AsyncUtil() {
    }

    public static <T> Handler<AsyncResult<T>> resolveFutureOnCompletion(Future<T> future) {
        return resolveFutureOnCompletion(future, null);
    }

    public static <T> Handler<AsyncResult<T>> resolveFutureOnCompletion(Future<T> future, T result) {
        return completion -> {
            if (completion.succeeded()) {
                if (nonNull(result)) {
                    future.complete(result);
                } else {
                    future.complete();
                }
            } else {
                future.fail(completion.cause());
            }
        };
    }
}
