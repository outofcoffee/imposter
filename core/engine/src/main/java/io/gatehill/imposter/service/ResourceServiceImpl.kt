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
package io.gatehill.imposter.service

import com.google.common.collect.Lists
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeHandler
import io.gatehill.imposter.http.ResourceMatcher
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.lifecycle.SecurityLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.RequestHeadersResourceConfig
import io.gatehill.imposter.server.RequestHandlingMode
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ResourceServiceImpl @Inject constructor(
    private val securityService: SecurityService,
    private val engineLifecycle: EngineLifecycleHooks,
    private val securityLifecycle: SecurityLifecycleHooks
) : ResourceService {

    private val shouldAddEngineResponseHeaders: Boolean

    init {
        shouldAddEngineResponseHeaders = EnvVars.getEnv("IMPOSTER_ADD_ENGINE_RESPONSE_HEADERS")?.toBoolean() != false
    }

    /**
     * {@inheritDoc}
     */
    override fun resolveResourceConfigs(pluginConfig: PluginConfig): List<ResolvedResourceConfig> {
        return (pluginConfig as? ResourcesHolder<*>)?.resources?.map { config ->
            ResolvedResourceConfig(
                config = config,
                pathParams = (config as? PathParamsResourceConfig)?.pathParams ?: emptyMap(),
                queryParams = (config as? QueryParamsResourceConfig)?.queryParams ?: emptyMap(),
                requestHeaders = (config as? RequestHeadersResourceConfig)?.requestHeaders ?: emptyMap()
            )
        } ?: emptyList()
    }

    /**
     * {@inheritDoc}
     */
    override fun handleRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        vertx: Vertx,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeHandler {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx, resourceMatcher, httpExchangeHandler)
    }

    /**
     * {@inheritDoc}
     */
    override fun handleRoute(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        vertx: Vertx,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeHandler {
        val resolvedResourceConfigs = resolveResourceConfigs(pluginConfig)
        return when (imposterConfig.requestHandlingMode) {
            RequestHandlingMode.SYNC -> { httpExchange: HttpExchange ->
                try {
                    handleResource(pluginConfig, httpExchangeHandler, httpExchange, resolvedResourceConfigs, resourceMatcher)
                } catch (e: Exception) {
                    handleFailure(httpExchange, e)
                }
            }
            RequestHandlingMode.ASYNC -> { httpExchange: HttpExchange ->
                val handler = Handler<Promise<Unit>> { promise ->
                    try {
                        handleResource(pluginConfig, httpExchangeHandler, httpExchange, resolvedResourceConfigs, resourceMatcher)
                        promise.complete()
                    } catch (e: Exception) {
                        promise.fail(e)
                    }
                }

                // explicitly disable ordered execution - responses should not block each other
                // as this causes head of line blocking performance issues
                vertx.orCreateContext.executeBlocking(handler, false) { result ->
                    if (result.failed()) {
                        handleFailure(httpExchange, result.cause())
                    }
                }
            }
            else -> throw UnsupportedOperationException("Unsupported request handling mode: " + imposterConfig.requestHandlingMode)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun passthroughRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        vertx: Vertx,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeHandler {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx, resourceMatcher) { event: HttpExchange ->
            httpExchangeHandler(event)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun buildNotFoundExceptionHandler() = { httpExchange: HttpExchange ->
        if (null == httpExchange.get(ResourceUtil.RC_REQUEST_ID_KEY)) {
            // only override response processing if the 404 did not originate from the mock engine
            logAppropriatelyForPath(httpExchange, "File not found")
            httpExchange.response().setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
        }

        // print summary
        LogUtil.logCompletion(httpExchange)
    }

    /**
     * {@inheritDoc}
     */
    override fun buildUnhandledExceptionHandler() = { httpExchange: HttpExchange ->
        logAppropriatelyForPath(httpExchange, "Unhandled routing exception for request")

        // print summary
        LogUtil.logCompletion(httpExchange)
    }

    private fun logAppropriatelyForPath(httpExchange: HttpExchange, description: String) {
        val level = determineLogLevel(httpExchange)
        LOGGER.log(
            level,
            "$description: ${describeRequest(httpExchange)}",
            httpExchange.failure()
        )
    }

    private fun determineLogLevel(httpExchange: HttpExchange): Level {
        return try {
            httpExchange.request().path().takeIf { path: String ->
                IGNORED_ERROR_PATHS.any { p: Pattern -> p.matcher(path).matches() }
            }?.let { Level.TRACE } ?: Level.ERROR

        } catch (ignored: Exception) {
            Level.ERROR
        }
    }

    private fun handleResource(
        pluginConfig: PluginConfig,
        httpExchangeHandler: HttpExchangeHandler,
        httpExchange: HttpExchange,
        resolvedResourceConfigs: List<ResolvedResourceConfig>,
        resourceMatcher: ResourceMatcher,
    ) {
        httpExchange.put(LogUtil.KEY_REQUEST_START, System.nanoTime())

        // every request has a unique ID
        val requestId = UUID.randomUUID().toString()
        httpExchange.put(ResourceUtil.RC_REQUEST_ID_KEY, requestId)

        val response = httpExchange.response()

        if (shouldAddEngineResponseHeaders) {
            response.putHeader("X-Imposter-Request", requestId)
            response.putHeader("Server", "imposter")
        }

        val rootResourceConfig = (pluginConfig as BasicResourceConfig?)!!

        val resourceConfig: BasicResourceConfig = resourceMatcher.matchResourceConfig(
            resolvedResourceConfigs,
            httpExchange,
        ) ?: rootResourceConfig

        // allows plugins to customise behaviour
        httpExchange.put(ResourceUtil.RESOURCE_CONFIG_KEY, resourceConfig)

        if (securityLifecycle.allMatch { listener: SecurityLifecycleListener ->
                listener.isRequestPermitted(rootResourceConfig, resourceConfig, resolvedResourceConfigs, httpExchange)
            }) {
            // request is permitted to continue
            try {
                httpExchangeHandler(httpExchange)
                LogUtil.logCompletion(httpExchange)
            } finally {
                // always perform tidy up once handled, regardless of outcome
                engineLifecycle.forEach { listener: EngineLifecycleListener ->
                    listener.afterHttpExchangeHandled(httpExchange, resourceConfig)
                }
            }
        } else {
            LOGGER.trace("Request {} was not permitted to continue", describeRequest(httpExchange, requestId))
        }
    }

    private fun handleFailure(httpExchange: HttpExchange, e: Throwable) {
        httpExchange.fail(
            RuntimeException("Unhandled exception processing request ${describeRequest(httpExchange)}", e)
        )
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ResourceServiceImpl::class.java)

        /**
         * Log errors for the following paths at TRACE instead of ERROR.
         */
        private val IGNORED_ERROR_PATHS: List<Pattern> = Lists.newArrayList(
            Pattern.compile(".*favicon\\.ico"),
            Pattern.compile(".*favicon.*\\.png")
        )
    }
}
