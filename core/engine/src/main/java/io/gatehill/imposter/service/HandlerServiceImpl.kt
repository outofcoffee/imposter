/*
 * Copyright (c) 2016-2024.
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
import io.gatehill.imposter.http.*
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.lifecycle.SecurityLifecycleListener
import io.gatehill.imposter.plugin.config.InterceptorsHolder
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.PassthroughResourceConfig
import io.gatehill.imposter.plugin.config.resource.UpstreamsHolder
import io.gatehill.imposter.server.ServerFactory
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.makeFuture
import io.gatehill.imposter.util.supervisedDefaultCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class HandlerServiceImpl @Inject constructor(
    private val securityService: SecurityService,
    private val securityLifecycle: SecurityLifecycleHooks,
    private val interceptorService: InterceptorService,
    private val responseService: ResponseService,
    private val upstreamService: UpstreamService,
) : HandlerService, CoroutineScope by supervisedDefaultCoroutineScope {

    private val shouldAddEngineResponseHeaders: Boolean =
        EnvVars.getEnv("IMPOSTER_ADD_ENGINE_RESPONSE_HEADERS")?.toBoolean() != false

    override fun build(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeFutureHandler,
    ): HttpExchangeFutureHandler {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return build(imposterConfig, selectedConfig, resourceMatcher, httpExchangeHandler)
    }

    override fun build(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeFutureHandler,
    ): HttpExchangeFutureHandler {
        val resolvedResourceConfigs = resolveResourceConfigs(pluginConfig)
        val resolvedInterceptorConfigs = resolveInterceptorConfigs(pluginConfig)
        return { httpExchange: HttpExchange ->
            handle(
                pluginConfig,
                httpExchangeHandler,
                httpExchange,
                resolvedResourceConfigs,
                resolvedInterceptorConfigs,
                resourceMatcher
            )
        }
    }

    override fun buildAndWrap(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeFutureHandler =
        build(imposterConfig, allPluginConfigs, resourceMatcher, wrapInFuture(httpExchangeHandler))

    override fun buildAndWrap(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeFutureHandler =
        build(imposterConfig, pluginConfig, resourceMatcher, wrapInFuture(httpExchangeHandler))

    /**
     * Wraps the given [httpExchangeHandler] in a [HttpExchangeFutureHandler] and returns the future.
     * Note that the future will be completed with the result of the block, so the block must
     * be a synchronous operation.
     */
    private fun wrapInFuture(
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeFutureHandler = { httpExchange ->
        makeFuture { httpExchangeHandler(httpExchange) }
    }

    override fun buildNotFoundExceptionHandler() = { httpExchange: HttpExchange ->
        if (
            null == httpExchange.get(ResourceUtil.RC_REQUEST_ID_KEY) ||
            httpExchange.get<Boolean>(ResourceUtil.RC_SEND_NOT_FOUND_RESPONSE) == true
        ) {
            // only override response processing if the 404 did not originate from the mock engine
            // otherwise this will attempt to send a duplicate response to an already completed
            // exchange, resulting in an IllegalStateException
            logAppropriatelyForPath(httpExchange, "File not found")
            responseService.sendNotFoundResponse(httpExchange)
        }

        // print summary
        LogUtil.logCompletion(httpExchange)
    }

    override fun buildUnhandledExceptionHandler() = { httpExchange: HttpExchange ->
        logAppropriatelyForPath(httpExchange, "Unhandled routing exception for request")

        // print summary
        LogUtil.logCompletion(httpExchange)
    }

    override fun handleStaticContent(
        serverFactory: ServerFactory,
        allConfigs: List<PluginConfig>,
        router: HttpRouter,
    ) {
        for (config in allConfigs) {
            if (config !is ResourcesHolder<*>) {
                continue
            }
            val resources = config.resources?.filter { ResourceUtil.isStaticContentRoute(it) } ?: emptyList()
            for (resource in resources) {
                if (resource.path.isNullOrBlank()) {
                    throw IllegalStateException("Static content dir [${resource.responseConfig.dir}] must specify path")
                }
                val path = resource.path!!
                if (resource.responseConfig.dir.isNullOrBlank()) {
                    throw IllegalStateException("Static content path [${path}] must specify dir")
                }
                if (!path.endsWith("/*")) {
                    throw IllegalStateException("Static content path [${path}] must end with a trailing slash")
                }
                if (resource.responseConfig.isTemplate == true) {
                    throw IllegalStateException("Static directory [${path}] cannot be a template")
                }
                if (!resource.responseConfig.file.isNullOrBlank()) {
                    throw IllegalStateException("Static directory [${path}] cannot specify a file")
                }
                if (!resource.responseConfig.content.isNullOrBlank()) {
                    throw IllegalStateException("Static directory [${path}] cannot specify content")
                }

                val method = ResourceUtil.extractResourceMethod(resource, HttpMethod.GET)!!
                val dir = resource.responseConfig.dir!!.let {
                    if (it.endsWith('/')) it.substringBeforeLast('/') else it
                }
                val absoluteDirPath = File(config.dir, dir).absolutePath

                LOGGER.debug("Adding static content handler for {} [{}] to directory [{}]", method, path, absoluteDirPath)
                router.route(method, path).handler(serverFactory.createStaticHttpHandler(absoluteDirPath, false))
            }
        }
    }

    /**
     * Extract the resource configurations from the plugin configuration, if present.
     *
     * @param pluginConfig the plugin configuration
     * @return the resource configurations
     */
    private fun resolveResourceConfigs(pluginConfig: PluginConfig): List<ResolvedResourceConfig> {
        return (pluginConfig as? ResourcesHolder<*>)?.resources?.map { config ->
            ResolvedResourceConfig.parse(config)
        } ?: emptyList()
    }

    /**
     * Extract the interceptor configurations from the plugin configuration, if present.
     *
     * @param pluginConfig the plugin configuration
     * @return the interceptor configurations
     */
    private fun resolveInterceptorConfigs(pluginConfig: PluginConfig): List<ResolvedResourceConfig> {
        return (pluginConfig as? InterceptorsHolder<*>)?.interceptors?.map { config ->
            ResolvedResourceConfig.parse(config)
        } ?: emptyList()
    }

    private fun logAppropriatelyForPath(httpExchange: HttpExchange, description: String) {
        val level = determineLogLevel(httpExchange)
        LOGGER.log(
            level,
            "$description: ${describeRequest(httpExchange)}",
            httpExchange.failureCause
        )
    }

    private fun determineLogLevel(httpExchange: HttpExchange): Level {
        return try {
            httpExchange.request.path.takeIf { path: String ->
                IGNORED_ERROR_PATHS.any { p: Pattern -> p.matcher(path).matches() }
            }?.let { Level.TRACE } ?: if (httpExchange.response.statusCode >= 500) Level.ERROR else Level.WARN

        } catch (ignored: Exception) {
            Level.ERROR
        }
    }

    private fun handle(
        pluginConfig: PluginConfig,
        httpExchangeHandler: HttpExchangeFutureHandler,
        httpExchange: HttpExchange,
        resourceConfigs: List<ResolvedResourceConfig>,
        interceptorConfigs: List<ResolvedResourceConfig>,
        resourceMatcher: ResourceMatcher,
    ): CompletableFuture<Unit> = future {
        try {
            httpExchange.put(LogUtil.KEY_REQUEST_START, System.nanoTime())

            // every request has a unique ID
            val requestId = UUID.randomUUID().toString()
            httpExchange.put(ResourceUtil.RC_REQUEST_ID_KEY, requestId)

            val response = httpExchange.response

            if (shouldAddEngineResponseHeaders) {
                response.putHeader("X-Imposter-Request", requestId)
                response.putHeader("Server", "imposter")
            }

            val matchedInterceptors = resourceMatcher.matchAllResourceConfigs(pluginConfig, interceptorConfigs, httpExchange)
            val rootResourceConfig = pluginConfig as BasicResourceConfig
            val resourceConfig = resourceMatcher.matchSingleResourceConfig(pluginConfig, resourceConfigs, httpExchange)
                ?: rootResourceConfig

            // allows plugins to customise behaviour
            httpExchange.put(ResourceUtil.RESOURCE_CONFIG_KEY, resourceConfig)

            if (isRequestPermitted(rootResourceConfig, resourceConfig, resourceConfigs, httpExchange)) {
                // set before actual dispatch to avoid race condition where
                // a response is sent before the phase is set
                httpExchange.phase = ExchangePhase.REQUEST_DISPATCHED

                val handled = interceptorService.executeInterceptors(
                    pluginConfig,
                    matchedInterceptors,
                    httpExchange
                ).await()

                if (!handled) {
                    if (shouldForwardToUpstream(pluginConfig, resourceConfig, httpExchange)) {
                        forwardToUpstream(pluginConfig, resourceConfig, httpExchange).await()
                    } else {
                        httpExchangeHandler(httpExchange).await()
                    }
                }
                LogUtil.logCompletion(httpExchange)

            } else {
                LOGGER.trace("Request {} was not permitted to continue", describeRequest(httpExchange, requestId))
            }

        } catch (e: Exception) {
            httpExchange.fail(
                RuntimeException("Unhandled exception processing request ${describeRequest(httpExchange)}", e)
            )
        }
    }

    private fun isRequestPermitted(
        rootResourceConfig: BasicResourceConfig,
        resourceConfig: BasicResourceConfig,
        resolvedResourceConfigs: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ) = securityLifecycle.allMatch { listener: SecurityLifecycleListener ->
        listener.isRequestPermitted(rootResourceConfig, resourceConfig, resolvedResourceConfigs, httpExchange)
    }

    private fun shouldForwardToUpstream(
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig,
        httpExchange: HttpExchange,
    ): Boolean {
        if (pluginConfig !is UpstreamsHolder || resourceConfig !is PassthroughResourceConfig || resourceConfig.passthrough.isNullOrBlank()) {
            return false
        }
        // don't forward requests to the upstream if the request is for system resources
        return !httpExchange.request.path.startsWith("/system")
    }

    private fun forwardToUpstream(
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig,
        httpExchange: HttpExchange,
    ) = upstreamService.forwardToUpstream(
        pluginConfig as UpstreamsHolder,
        resourceConfig as PassthroughResourceConfig,
        httpExchange
    )

    companion object {
        private val LOGGER = LogManager.getLogger(HandlerServiceImpl::class.java)

        /**
         * Log errors for the following paths at TRACE instead of ERROR.
         */
        private val IGNORED_ERROR_PATHS: List<Pattern> = Lists.newArrayList(
            Pattern.compile(".*favicon\\.ico"),
            Pattern.compile(".*favicon.*\\.png")
        )
    }
}
