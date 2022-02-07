package io.gatehill.imposter.util

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Invoke the given block [attempts] times. If a [Throwable] is
 * thrown, retry with a delay of [delayMs] ms.
 */
fun attempt(attempts: Int, delayMs: Long = 500, block: () -> Unit) {
    Vertx.currentContext().executeBlocking<Unit>({
        runBlocking {
            for (attempt in 1..attempts) {
                try {
                    block()
                } catch (e: Throwable) {
                    if (attempt >= attempts) {
                        throw RuntimeException("Failed after $attempt attempts", e)
                    } else {
                        delay(delayMs)
                    }
                }
            }

        }
    }, {})
}
