/*
 * Copyright (c) 2023-2024.
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

package io.gatehill.imposter.service.security

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.ResourceMatcher
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.security.CorsConfig
import io.gatehill.imposter.plugin.config.security.CorsConfigHolder
import io.gatehill.imposter.service.HandlerService
import io.gatehill.imposter.service.SecurityService
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * Handles CORS requests.
 */
class CorsService @Inject constructor(
    private val securityService: SecurityService,
    private val handlerService: HandlerService,
) {
    private val logger = LogManager.getLogger(CorsService::class.java)

    fun configure(
        imposterConfig: ImposterConfig,
        allConfigs: List<PluginConfig>,
        router: HttpRouter,
        resourceMatcher: ResourceMatcher,
    ) {
        if (allConfigs.any { it is CorsConfigHolder && it.corsConfig != null }) {
            logger.debug("CORS enabled")
            val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allConfigs)
            if (selectedConfig !is CorsConfigHolder || selectedConfig.corsConfig == null) {
                throw IllegalStateException("No CORS configuration found")
            }
            val cors = selectedConfig.corsConfig!!

            // preflight
            router.route(HttpMethod.OPTIONS, "/*").handler(
                handlePreflight(imposterConfig, selectedConfig, resourceMatcher, cors)
            )

            // request already handled
            router.onBeforeEnd(decorate(cors))

        } else {
            logger.trace("CORS disabled")
        }
    }
    
    private fun handlePreflight(
        imposterConfig: ImposterConfig,
        selectedConfig: PluginConfig,
        resourceMatcher: ResourceMatcher,
        cors: CorsConfig,
    ): HttpExchangeFutureHandler {
        return handlerService.buildAndWrap(imposterConfig, selectedConfig, resourceMatcher) { exchange: HttpExchange ->
            val origin = determineResponseOrigin(cors, exchange.request)
            origin?.let {
                logger.debug("Serving CORS pre-flight request: ${LogUtil.describeRequest(exchange)}")
                exchange.response.setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                addCorsHeaders(exchange, origin, cors)
                exchange.response.end()
            } ?: run {
                logger.warn("CORS pre-flight request with invalid Origin: ${LogUtil.describeRequest(exchange)}")
                exchange.response
                    .setStatusCode(HttpUtil.HTTP_BAD_REQUEST)
                    .end()
            }
        }
    }

    private fun decorate(cors: CorsConfig) = { exchange: HttpExchange ->
        // don't add headers to preflight requests (handled in preflight handler)
        if (exchange.request.method != HttpMethod.OPTIONS) {
            val origin = determineResponseOrigin(cors, exchange.request)
            origin?.let {
                logger.trace("Adding CORS headers to response for: ${LogUtil.describeRequest(exchange)}")
                addCorsHeaders(exchange, origin, cors)
            } ?: run {
                logger.trace("CORS request with invalid Origin: ${LogUtil.describeRequest(exchange)}")
            }
        }
    }

    private fun determineResponseOrigin(cors: CorsConfig, request: HttpRequest): String? {
        val requestOrigin = request.getHeader("Origin")
        val allowOrigins = when (cors.allowOrigins) {
            is String -> listOf(cors.allowOrigins)
            is List<*> -> (cors.allowOrigins as List<*>).map { it.toString() }
            else -> null
        }
        return allowOrigins?.let {
            if (it.contains(requestOrigin) || it.contains(MatchRequestOrigin)) {
                requestOrigin
            } else if (it.contains(WildcardOrigin)) {
                WildcardOrigin
            } else {
                null
            }
        }
    }

    private fun addCorsHeaders(exchange: HttpExchange, origin: String, cors: CorsConfig) {
        exchange.response
            .putHeader(HttpUtil.CORS_ALLOW_ORIGIN, origin)
            .putHeader("Access-Control-Allow-Methods", cors.allowMethods?.joinToString(",") ?: "*")
            .putHeader("Access-Control-Allow-Headers", cors.allowHeaders?.joinToString(",") ?: "*")
            .putHeader("Access-Control-Allow-Credentials", cors.allowCredentials.toString())
            .putHeader("Access-Control-Max-Age", cors.maxAge.toString())
    }

    companion object {
        private const val WildcardOrigin = "*"
        private const val MatchRequestOrigin = "all"
    }
}
