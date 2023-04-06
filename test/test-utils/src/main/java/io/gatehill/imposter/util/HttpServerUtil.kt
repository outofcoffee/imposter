package io.gatehill.imposter.util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

object HttpServerUtil {
    /**
     * Starts an HTTP server on a random port.
     */
    fun listen(vertx: Vertx, requestHandler: (HttpServerRequest) -> Unit): Int {
        val listenPort = ServerSocket(0).use { it.localPort }
        val httpServer = vertx.createHttpServer(HttpServerOptions().setPort(listenPort))
        httpServer.requestHandler(requestHandler)
        blockWait { listenHandler: Handler<AsyncResult<HttpServer?>?>? -> httpServer.listen(listenHandler) }
        return listenPort
    }

    /**
     * Block the consumer until the handler is called.
     *
     * @param handlerConsumer the consumer of the handler
     * @param <T>             the type of the async result
    </T> */
    @Throws(Exception::class)
    fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
        val latch = CountDownLatch(1)
        val handler = Handler { _: T -> latch.countDown() }
        handlerConsumer.accept(handler)
        latch.await()
    }
}
