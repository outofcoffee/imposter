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

import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeHandler
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.lifecycle.SecurityLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.RequestHeadersResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig
import io.gatehill.imposter.server.RequestHandlingMode
import io.gatehill.imposter.util.CollectionUtil.convertKeysToLowerCase
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.StringUtil.safeEquals
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.util.Locale
import java.util.Objects
import java.util.UUID
import java.util.function.Function
import java.util.function.Supplier
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
    override fun matchResourceConfig(
        resources: List<ResolvedResourceConfig>,
        method: ResourceMethod,
        pathTemplate: String?,
        path: String?,
        pathParams: Map<String, String>,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
        bodySupplier: Supplier<String?>
    ): BasicResourceConfig? {
        var resourceConfigs = resources.filter { res ->
            isRequestMatch(
                res,
                method,
                pathTemplate,
                path,
                pathParams,
                queryParams,
                requestHeaders,
                bodySupplier
            )
        }

        // find the most specific, by filtering those that match by those that specify parameters
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::pathParams)
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::queryParams)
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::requestHeaders)

        if (resourceConfigs.isEmpty()) {
            return null
        }
        if (resourceConfigs.size == 1) {
            LOGGER.debug("Matched response config for {} {}", method, path)
        } else {
            LOGGER.warn(
                "More than one response config found for {} {} - this is probably a configuration error. Choosing first response configuration.",
                method,
                path
            )
        }
        return resourceConfigs[0].config
    }

    private fun filterByPairs(
        resourceConfigs: List<ResolvedResourceConfig>,
        pairsSupplier: Function<ResolvedResourceConfig, Map<String, String>>
    ): List<ResolvedResourceConfig> {
        val configsWithPairs = resourceConfigs.filter { res -> pairsSupplier.apply(res).isNotEmpty() }

        return configsWithPairs.ifEmpty {
            // no resource configs specified params - don't filter
            resourceConfigs
        }
    }

    /**
     * Determine if the resource configuration matches the current request.
     *
     * @param resource       the resource configuration
     * @param resourceMethod the HTTP method of the current request
     * @param pathTemplate   request path template
     * @param path           the path of the current request
     * @param pathParams     the path parameters of the current request
     * @param queryParams    the query parameters of the current request
     * @param requestHeaders the headers of the current request
     * @param bodySupplier   supplies the request body
     * @return `true` if the resource matches the request, otherwise `false`
     */
    private fun isRequestMatch(
        resource: ResolvedResourceConfig,
        resourceMethod: ResourceMethod,
        pathTemplate: String?,
        path: String?,
        pathParams: Map<String, String>,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
        bodySupplier: Supplier<String?>
    ): Boolean {
        val resourceConfig = resource.config

        // path template can be null when a regex route is used
        val pathMatch = path == resourceConfig.path || (pathTemplate?.let { it == resourceConfig.path } == true)

        val methodMatch = if (resourceConfig is MethodResourceConfig) {
            resourceMethod == resourceConfig.method
        } else {
            // unspecified implies any match
            true
        }

        return pathMatch && methodMatch &&
            matchPairs(pathParams, resource.pathParams, true) &&
            matchPairs(queryParams, resource.queryParams, true) &&
            matchPairs(requestHeaders, resource.requestHeaders, false) &&
            matchRequestBody(bodySupplier, resource.config)
    }

    /**
     * If the resource contains parameter configuration, check they are all present.
     * If the configuration contains no parameters, then this evaluates to true.
     * Additional parameters not in the configuration are ignored.
     *
     * @param resourceMap           the configured parameters to match
     * @param requestMap            the parameters from the request (e.g. query or path)
     * @param caseSensitiveKeyMatch whether to match keys case-sensitively
     * @return `true` if the configured parameters match the request, otherwise `false`
     */
    private fun matchPairs(
        requestMap: Map<String, String>,
        resourceMap: Map<String, String>,
        caseSensitiveKeyMatch: Boolean
    ): Boolean {
        // none configured - implies any match
        if (resourceMap.isEmpty()) {
            return true
        }
        val comparisonMap = if (caseSensitiveKeyMatch) requestMap else convertKeysToLowerCase(requestMap)
        return resourceMap.entries.any { (key, value) ->
            val configKey: String = if (caseSensitiveKeyMatch) key else key.lowercase(Locale.getDefault())
            safeEquals(comparisonMap[configKey], value)
        }
    }

    /**
     * Match the request body against the supplied configuration.
     *
     * @param bodySupplier         supplies the request body
     * @param resourceConfig the match configuration
     * @return `true` if the configuration is empty, or the request body matches the configuration, otherwise `false`
     */
    private fun matchRequestBody(bodySupplier: Supplier<String?>, resourceConfig: BasicResourceConfig): Boolean {
        if (resourceConfig !is RequestBodyResourceConfig ||
            Objects.isNull(resourceConfig.requestBody) ||
            Strings.isNullOrEmpty(resourceConfig.requestBody!!.jsonPath)
        ) {
            // none configured - implies any match
            return true
        }

        val requestBodyConfig = resourceConfig.requestBody!!
        val body = bodySupplier.get()
        val bodyValue = if (Strings.isNullOrEmpty(body)) {
            null
        } else {
            try {
                JsonPath.read<Any>(body, requestBodyConfig.jsonPath)
            } catch (ignored: PathNotFoundException) {
                null
            }
        }
        return safeEquals(requestBodyConfig.value, bodyValue)
    }

    /**
     * {@inheritDoc}
     */
    override fun handleRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        vertx: Vertx,
        httpExchangeHandler: HttpExchangeHandler
    ): HttpExchangeHandler {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx, httpExchangeHandler)
    }

    /**
     * {@inheritDoc}
     */
    override fun handleRoute(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        vertx: Vertx,
        httpExchangeHandler: HttpExchangeHandler
    ): HttpExchangeHandler {
        val resolvedResourceConfigs = resolveResourceConfigs(pluginConfig)
        return when (imposterConfig.requestHandlingMode) {
            RequestHandlingMode.SYNC -> { httpExchange: HttpExchange ->
                try {
                    handleResource(pluginConfig, httpExchangeHandler, httpExchange, resolvedResourceConfigs)
                } catch (e: Exception) {
                    handleFailure(httpExchange, e)
                }
            }
            RequestHandlingMode.ASYNC -> { httpExchange: HttpExchange ->
                val handler = Handler<Promise<Unit>> { promise ->
                    try {
                        handleResource(pluginConfig, httpExchangeHandler, httpExchange, resolvedResourceConfigs)
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
        httpExchangeHandler: HttpExchangeHandler
    ): HttpExchangeHandler {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx) { event: HttpExchange ->
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
        resolvedResourceConfigs: List<ResolvedResourceConfig>
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
        val request = httpExchange.request()

        val resourceConfig: BasicResourceConfig = matchResourceConfig(
            resolvedResourceConfigs,
            request.method(),
            httpExchange.currentRoutePath,
            request.path(),
            httpExchange.pathParams(),
            httpExchange.queryParams(),
            request.headers(),
            { httpExchange.bodyAsString }
        )?: rootResourceConfig

        // allows plugins to customise behaviour
        httpExchange.put(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY, resourceConfig)

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
