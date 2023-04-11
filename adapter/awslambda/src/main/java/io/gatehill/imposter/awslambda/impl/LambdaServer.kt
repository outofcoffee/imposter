/*
 * Copyright (c) 2021-2023.
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

package io.gatehill.imposter.awslambda.impl

import io.gatehill.imposter.awslambda.impl.model.LambdaHttpExchange
import io.gatehill.imposter.awslambda.impl.model.LambdaHttpResponse
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.server.HttpServer
import io.gatehill.imposter.service.ResponseService
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Collections.synchronizedMap

/**
 * @author Pete Cornish
 */
abstract class LambdaServer<Request, Response>(
    private val responseService: ResponseService,
    private val router: HttpRouter,
) : HttpServer {
    protected val logger: Logger = LogManager.getLogger(LambdaServer::class.java)
    private val routes: Array<HttpRoute>
    private val errorHandlers: Map<Int, (HttpExchange) -> Unit>

    init {
        routes = router.routes.toTypedArray()
        errorHandlers = synchronizedMap(router.errorHandlers)
    }

    fun dispatch(event: Request): Response {
        val response = LambdaHttpResponse()
        var failureCause: Throwable? = null
        try {
            val matched = matchRoutes(event)

            if (matched.isEmpty() || matched.all { it.isCatchAll() }) {
                logger.trace("No explicit routes matched for: ${describeRequestShort(event)}")
                val request = buildRequest(event, null)
                val exchange = LambdaHttpExchange(router, request, response, null)
                responseService.sendNotFoundResponse(exchange)

            } else {
                matched.forEach { route ->
                    val request = buildRequest(event, route)
                    val exchange = LambdaHttpExchange(router, request, response, route)
                    val handler = route.handler ?: throw IllegalStateException("No route handler set for: $route")
                    try {
                        handler(exchange)
                    } catch (e: Exception) {
                        throw RuntimeException("Unhandled error in route: $route", e)
                    }
                    // check for route failure
                    exchange.failure()?.let { cause ->
                        throw RuntimeException("Route failed: $route", cause)
                    }
                }
            }

        } catch (e: Exception) {
            failureCause = e
        }

        when (val statusCode = response.statusCode) {
            in 400..499 -> {
                errorHandlers[statusCode]?.let { errorHandler ->
                    failExchange(event, response, statusCode, null, errorHandler)
                } ?: logger.warn("Unhandled client error for: ${describeRequestShort(event)} [status code: $statusCode]")
            }
            in 500..599 -> {
                errorHandlers[statusCode]?.let { errorHandler ->
                    failExchange(event, response, statusCode, failureCause, errorHandler)
                } ?: logger.error("Unhandled server error for: ${describeRequestShort(event)} [status code: $statusCode]", failureCause)
            }
        }

        return buildResponse(response)
    }

    private fun failExchange(
        event: Request,
        response: LambdaHttpResponse,
        statusCode: Int,
        failureCause: Throwable?,
        errorHandler: (HttpExchange) -> Unit,
    ) {
        val request = buildRequest(event, null)
        val exchange = LambdaHttpExchange(router, request, response, null)
        exchange.fail(statusCode, failureCause)
        errorHandler(exchange)
    }

    private fun matchRoutes(event: Request): List<HttpRoute> {
        val requestPath = getRequestPath(event)
        val requestMethod = getRequestMethod(event)

        val matchedRoutes = routes.filter { route ->
            route.matches(requestPath) && (null == route.method || requestMethod == route.method.toString())
        }
        if (logger.isTraceEnabled) {
            logger.trace("Routes matched for: ${describeRequestShort(event)}: $matchedRoutes")
        }
        return matchedRoutes
    }

    override fun close(onCompletion: Handler<AsyncResult<Void>>) {
        /* no op */
    }

    private fun describeRequestShort(event: Request): String {
        return getRequestMethod(event) + " " + getRequestPath(event)
    }

    protected abstract fun getRequestMethod(event: Request): String
    protected abstract fun getRequestPath(event: Request): String
    protected abstract fun acceptsHtml(event: Request): Boolean
    protected abstract fun buildRequest(event: Request, route: HttpRoute?): HttpRequest
    protected abstract fun buildResponse(response: LambdaHttpResponse): Response
}
