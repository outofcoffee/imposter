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
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.lifecycle.SecurityLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.RequestHeadersResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig
import io.gatehill.imposter.server.RequestHandlingMode
import io.gatehill.imposter.util.CollectionUtil
import io.gatehill.imposter.util.CollectionUtil.convertKeysToLowerCase
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.StringUtil.safeEquals
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.function.Consumer
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

    /**
     * {@inheritDoc}
     */
    override fun resolveResourceConfigs(pluginConfig: PluginConfig): List<ResolvedResourceConfig> {
        if (pluginConfig is ResourcesHolder<*>) {
            val resources = pluginConfig as ResourcesHolder<*>
            if (Objects.nonNull(resources.resources)) {
                return resources.resources!!.map { res: RestResourceConfig ->
                    ResolvedResourceConfig(res, findPathParams(res), findQueryParams(res), findRequestHeaders(res))
                }
            }
        }
        return emptyList()
    }

    private fun findPathParams(resourceConfig: ResourceConfig): Map<String, String> {
        if (resourceConfig is PathParamsResourceConfig) {
            val params = (resourceConfig as PathParamsResourceConfig).pathParams
            return params ?: emptyMap()
        }
        return emptyMap()
    }

    private fun findQueryParams(resourceConfig: ResourceConfig): Map<String, String> {
        if (resourceConfig is QueryParamsResourceConfig) {
            val params = (resourceConfig as QueryParamsResourceConfig).queryParams
            return params ?: emptyMap()
        }
        return emptyMap()
    }

    private fun findRequestHeaders(resourceConfig: ResourceConfig): Map<String, String> {
        if (resourceConfig is RequestHeadersResourceConfig) {
            val headers = (resourceConfig as RequestHeadersResourceConfig).requestHeaders
            return headers ?: emptyMap()
        }
        return emptyMap()
    }

    /**
     * {@inheritDoc}
     */
    override fun matchResourceConfig(
        resources: List<ResolvedResourceConfig>,
        method: HttpMethod,
        pathTemplate: String?,
        path: String?,
        pathParams: Map<String, String>,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
        bodySupplier: Supplier<String?>
    ): ResponseConfigHolder? {
        val resourceMethod = ResourceUtil.convertMethodFromVertx(method)
        var resourceConfigs = resources.filter { res ->
            isRequestMatch(
                res,
                resourceMethod,
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
            LOGGER.debug("Matched response config for {} {}", resourceMethod, path)
        } else {
            LOGGER.warn(
                "More than one response config found for {} {} - this is probably a configuration error. Choosing first response configuration.",
                resourceMethod,
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

        resourceConfig.method ?: LOGGER.warn(
            "Resource configuration for '{}' is missing HTTP method - will not correctly match response behaviour",
            resourceConfig.path
        )

        return pathMatch && resourceMethod == resourceConfig.method &&
                matchPairs(pathParams, resource.pathParams, true) &&
                matchPairs(queryParams, resource.queryParams, true) &&
                matchPairs(requestHeaders, resource.requestHeaders, false) &&
                matchRequestBody(bodySupplier, resource.config.requestBody)
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
     * @param bodySupplier      supplies the request body
     * @param requestBodyConfig the match configuration
     * @return `true` if the configuration is empty, or the request body matches the configuration, otherwise `false`
     */
    private fun matchRequestBody(bodySupplier: Supplier<String?>, requestBodyConfig: RequestBodyConfig?): Boolean {
        // none configured - implies any match
        if (Objects.isNull(requestBodyConfig) || Strings.isNullOrEmpty(requestBodyConfig!!.jsonPath)) {
            return true
        }
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
        routingContextConsumer: Consumer<RoutingContext>
    ): Handler<RoutingContext> {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx, routingContextConsumer)
    }

    /**
     * {@inheritDoc}
     */
    override fun handleRoute(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        vertx: Vertx,
        routingContextConsumer: Consumer<RoutingContext>
    ): Handler<RoutingContext> {
        val resolvedResourceConfigs = resolveResourceConfigs(pluginConfig)
        return when (imposterConfig.requestHandlingMode) {
            RequestHandlingMode.SYNC -> Handler { routingContext: RoutingContext ->
                try {
                    handleResource(pluginConfig, routingContextConsumer, routingContext, resolvedResourceConfigs)
                } catch (e: Exception) {
                    handleFailure(routingContext, e)
                }
            }
            RequestHandlingMode.ASYNC -> Handler { routingContext: RoutingContext ->
                val handler = Handler { future: Future<Any?> ->
                    try {
                        handleResource(pluginConfig, routingContextConsumer, routingContext, resolvedResourceConfigs)
                        future.complete()
                    } catch (e: Exception) {
                        future.fail(e)
                    }
                }

                // explicitly disable ordered execution - responses should not block each other
                // as this causes head of line blocking performance issues
                vertx.orCreateContext.executeBlocking(handler, false) { result: AsyncResult<Any?> ->
                    if (result.failed()) {
                        handleFailure(routingContext, result.cause())
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
        routingContextHandler: Handler<RoutingContext>
    ): Handler<RoutingContext> {
        val selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs)
        return handleRoute(imposterConfig, selectedConfig, vertx) { event: RoutingContext ->
            routingContextHandler.handle(event)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun buildUnhandledExceptionHandler() = Handler { routingContext: RoutingContext ->
        val level = determineLogLevel(routingContext)
        LOGGER.log(
            level,
            "Unhandled routing exception for request " + describeRequest(routingContext),
            routingContext.failure()
        )
    }

    private fun determineLogLevel(routingContext: RoutingContext): Level {
        return try {
            routingContext.request().path()?.takeIf { path: String ->
                IGNORED_ERROR_PATHS.stream().anyMatch { p: Pattern -> p.matcher(path).matches() }
            }?.let { Level.TRACE } ?: Level.ERROR

        } catch (ignored: Exception) {
            Level.ERROR
        }
    }

    private fun handleResource(
        pluginConfig: PluginConfig,
        routingContextConsumer: Consumer<RoutingContext>,
        routingContext: RoutingContext,
        resolvedResourceConfigs: List<ResolvedResourceConfig>
    ) {
        // every request has a unique ID
        val requestId = UUID.randomUUID().toString()
        routingContext.put(ResourceUtil.RC_REQUEST_ID_KEY, requestId)
        val response = routingContext.response()
        response.putHeader("X-Imposter-Request", requestId)
        response.putHeader("Server", "imposter")

        val rootResourceConfig = (pluginConfig as ResponseConfigHolder?)!!
        val request = routingContext.request()
        val resourceConfig = matchResourceConfig(
            resolvedResourceConfigs,
            request.method(),
            routingContext.currentRoute().path,
            request.path(),
            routingContext.pathParams(),
            CollectionUtil.asMap(request.params()),
            CollectionUtil.asMap(request.headers())
        ) { routingContext.bodyAsString } ?: rootResourceConfig

        // allows plugins to customise behaviour
        routingContext.put(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY, resourceConfig)
        if (securityLifecycle.allMatch { listener: SecurityLifecycleListener ->
                listener.isRequestPermitted(rootResourceConfig, resourceConfig, resolvedResourceConfigs, routingContext)
            }) {
            // request is permitted to continue
            try {
                routingContextConsumer.accept(routingContext)
            } finally {
                // always perform tidy up once handled, regardless of outcome
                engineLifecycle.forEach { listener: EngineLifecycleListener ->
                    listener.afterRoutingContextHandled(routingContext)
                }
            }
        } else {
            LOGGER.trace("Request {} was not permitted to continue", describeRequest(routingContext, requestId))
        }
    }

    private fun handleFailure(routingContext: RoutingContext, e: Throwable) {
        routingContext.fail(
            RuntimeException(
                "Unhandled exception processing request ${describeRequest(routingContext)}", e
            )
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