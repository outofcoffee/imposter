/*
 * Copyright (c) 2021-2024.
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

import com.google.inject.Injector
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl
import io.gatehill.imposter.server.HttpServer
import io.gatehill.imposter.server.ServerFactory
import io.gatehill.imposter.service.ResponseFileService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.util.makeFuture
import io.vertx.core.Vertx
import io.vertx.core.http.impl.HttpUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class LambdaServerFactory @Inject constructor(
    private val responseService: ResponseService,
    private val responseFileService: ResponseFileService,
) : ServerFactory {
    private val logger = LogManager.getLogger(LambdaServerFactory::class.java)

    lateinit var activeServer: LambdaServer<*, *>
        private set

    override fun provide(injector: Injector, imposterConfig: ImposterConfig, vertx: Vertx, router: HttpRouter): CompletableFuture<HttpServer> {
        val responseService = injector.getInstance(ResponseService::class.java)
        activeServer = when (eventType) {
            EventType.ApiGatewayV1 -> ServerV1(responseService, router)
            EventType.ApiGatewayV2 -> ServerV2(responseService, router)
        }
        return CompletableFuture.completedFuture(activeServer)
    }

    override fun createBodyHttpHandler(): HttpExchangeFutureHandler = {
        CompletableFuture.completedFuture(Unit)
    }

    override fun createStaticHttpHandler(root: String, relative: Boolean): HttpExchangeFutureHandler {
        val pluginConfig = PluginConfigImpl().apply {
            plugin = "rest"
            dir = File(root)
        }
        return { exchange ->
            makeFuture {
                var path = exchange.request.path.let { HttpUtils.removeDots(it) }
                exchange.currentRoute?.let { currentRoute ->
                    val routePath = currentRoute.path
                    if (routePath != null && currentRoute.hasTrailingWildcard) {
                        path = path.removePrefix(routePath.removeSuffix("*"))
                    }
                }
                if (path.endsWith('/') || path.isEmpty()) {
                    path += indexFile
                }
                logger.debug("Serving static resource: $path")
                responseService.sendThenFinaliseExchange(null, exchange) {
                    val responseBehaviour = ReadWriteResponseBehaviourImpl().apply {
                        responseFile = path
                    }
                    responseFileService.serveResponseFile(pluginConfig, null, exchange, responseBehaviour)
                }
            }
        }
    }

    override fun createMetricsHandler(): HttpExchangeFutureHandler = {
        CompletableFuture.completedFuture(Unit)
    }

    companion object {
        private const val indexFile = "index.html"
        lateinit var eventType: EventType
    }

    enum class EventType {
        ApiGatewayV1,
        ApiGatewayV2,
    }
}
