/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.gatehill.imposter.server

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpRequestHandler
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.util.AsyncUtil
import io.gatehill.imposter.util.CollectionUtil
import io.gatehill.imposter.util.FileUtil
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.ResourceUtil.convertMethodToVertx
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.MIMEHeader
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.impl.BodyHandlerImpl
import io.vertx.micrometer.PrometheusScrapingHandler
import org.apache.logging.log4j.LogManager
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
class VertxWebServerFactoryImpl : ServerFactory {
    override fun provide(
        imposterConfig: ImposterConfig,
        startFuture: Future<*>,
        vertx: Vertx,
        router: HttpRouter
    ): HttpServer {
        LOGGER.trace("Starting mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        val serverOptions = HttpServerOptions()

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled) {
            LOGGER.trace("TLS is enabled")

            // locate keystore
            val keystorePath: Path = if (imposterConfig.keystorePath!!.startsWith(FileUtil.CLASSPATH_PREFIX)) {
                try {
                    val kp = imposterConfig.keystorePath!!.substring(FileUtil.CLASSPATH_PREFIX.length)
                    Paths.get(VertxWebServerFactoryImpl::class.java.getResource(kp).toURI())
                } catch (e: URISyntaxException) {
                    throw RuntimeException("Error locating keystore", e)
                }
            } else {
                Paths.get(imposterConfig.keystorePath!!)
            }

            val jksOptions = JksOptions()
            jksOptions.path = keystorePath.toString()
            jksOptions.password = imposterConfig.keystorePassword
            serverOptions.keyStoreOptions = jksOptions
            serverOptions.isSsl = true

        } else {
            LOGGER.trace("TLS is disabled")
        }

        LOGGER.trace("Listening on {}", imposterConfig.serverUrl)

        val vertxRouter = convertRouterToVertx(router)
        val vertxServer = vertx.createHttpServer(serverOptions)
            .requestHandler(vertxRouter)
            .listen(imposterConfig.listenPort, imposterConfig.host, AsyncUtil.resolveFutureOnCompletion(startFuture))

        return VertxHttpServer(vertxServer)
    }

    private fun convertRouterToVertx(router: HttpRouter) = Router.router(router.vertx).also { vr ->
        router.routes.forEach { hr ->
            val route = hr.regex?.let { regex ->
                hr.method?.let { method -> vr.routeWithRegex(convertMethodToVertx(method), regex) }
                    ?: vr.routeWithRegex(regex)

            } ?: hr.path?.let { path ->
                hr.method?.let { method -> vr.route(convertMethodToVertx(method), path) } ?: vr.route(path)

            } ?: vr.route()

            route.handler { rc ->
                hr.handler!!.invoke(VertxHttpExchange(rc, rc.currentRoute().path))
            }
        }

        router.errorHandlers.forEach { eh ->
            vr.errorHandler(eh.key) { rc ->
                eh.value.invoke(VertxHttpExchange(rc, rc.currentRoute().path))
            }
        }
    }

    override fun createBodyHttpHandler(): HttpRequestHandler {
        val handler = BodyHandlerImpl()
        return { he -> handler.handle((he as VertxHttpExchange).routingContext) }
    }

    override fun createStaticHttpHandler(root: String): HttpRequestHandler {
        val handler = StaticHandler.create(root)
        return { he -> handler.handle((he as VertxHttpExchange).routingContext) }
    }

    override fun createMetricsHandler(): HttpRequestHandler {
        val handler = PrometheusScrapingHandler.create()
        return { he -> handler.handle((he as VertxHttpExchange).routingContext) }
    }

    class VertxHttpServer(private val vertxServer: io.vertx.core.http.HttpServer) : HttpServer {
        override fun close(onCompletion: Handler<AsyncResult<Void>>) {
            vertxServer.close(onCompletion)
        }
    }

    class VertxHttpExchange(
        val routingContext: RoutingContext,
        override val currentRoutePath: String?
    ) : HttpExchange {

        override fun request(): HttpRequest {
            return VertxHttpRequest(routingContext.request())
        }

        override fun response(): HttpResponse {
            return VertxHttpResponse(routingContext.response())
        }

        override fun pathParam(paramName: String): String? {
            return routingContext.pathParam(paramName)
        }

        override fun queryParam(queryParam: String): String? {
            return routingContext.queryParam(queryParam)?.firstOrNull()
        }

        override fun parsedAcceptHeader(): List<MIMEHeader> {
            return routingContext.parsedHeaders().accept()
        }

        override val body: Buffer? by lazy { routingContext.body }

        override val bodyAsString: String? by lazy { routingContext.bodyAsString }

        override val bodyAsJson: JsonObject? by lazy { routingContext.bodyAsJson }

        override fun queryParams(): Map<String, String> {
            return CollectionUtil.asMap(routingContext.queryParams())
        }

        override fun pathParams(): Map<String, String> {
            return routingContext.pathParams()
        }

        override fun fail(cause: Throwable?) {
            routingContext.fail(cause)
        }

        override fun fail(statusCode: Int) {
            routingContext.fail(statusCode)
        }

        override fun failure(): Throwable? {
            return routingContext.failure()
        }

        override fun <T> get(key: String): T? {
            return routingContext.get<T>(key)
        }

        override fun put(key: String, value: Any) {
            routingContext.put(key, value)
        }
    }

    class VertxHttpRequest(private val vertxRequest: HttpServerRequest) : HttpRequest {
        override fun path(): String {
            return vertxRequest.path() ?: ""
        }

        override fun method(): ResourceMethod {
            return ResourceUtil.convertMethodFromVertx(vertxRequest.method())
        }

        override fun absoluteURI(): String {
            return vertxRequest.absoluteURI()
        }

        override fun headers(): Map<String, String> {
            return CollectionUtil.asMap(vertxRequest.headers())
        }

        override fun getHeader(headerKey: String): String? {
            return vertxRequest.getHeader(headerKey)
        }
    }

    class VertxHttpResponse(val vertxResponse: HttpServerResponse) : HttpResponse {
        override fun setStatusCode(statusCode: Int): HttpResponse {
            vertxResponse.statusCode = statusCode
            return this
        }

        override fun getStatusCode(): Int {
            return vertxResponse.statusCode
        }

        override fun putHeader(headerKey: String, headerValue: String): HttpResponse {
            vertxResponse.putHeader(headerKey, headerValue)
            return this
        }

        override fun headers(): MultiMap {
            return vertxResponse.headers()
        }

        override fun sendFile(filePath: String): HttpResponse {
            vertxResponse.sendFile(filePath)
            return this
        }

        override fun end() {
            vertxResponse.end()
        }

        override fun end(body: String?) {
            body?.let { vertxResponse.end(body) } ?: end()
        }

        override fun end(body: Buffer) {
            vertxResponse.end(body)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VertxWebServerFactoryImpl::class.java)
    }
}
