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
package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.StatusCodeFactory
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.ScriptedPlugin.scriptHandler
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.plugin.openapi.http.OpenApiResponseBehaviourFactory
import io.gatehill.imposter.plugin.openapi.loader.SpecificationLoader
import io.gatehill.imposter.plugin.openapi.service.ExampleService
import io.gatehill.imposter.plugin.openapi.service.SpecificationService
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil.describeRequestShort
import io.gatehill.imposter.util.MapUtil.addJavaTimeSupport
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.ResourceUtil.convertPathToVertx
import io.swagger.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.responses.ApiResponse
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import org.apache.logging.log4j.LogManager
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Consumer
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish
 */
@PluginInfo("openapi")
@RequireModules(OpenApiModule::class)
class OpenApiPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val specificationService: SpecificationService,
    private val exampleService: ExampleService,
    private val responseService: ResponseService,
    private val openApiResponseBehaviourFactory: OpenApiResponseBehaviourFactory
) : ConfiguredPlugin<OpenApiPluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = OpenApiPluginConfig::class.java
    private var allSpecs: MutableList<OpenAPI>? = null

    companion object {
        private val LOGGER = LogManager.getLogger(OpenApiPluginImpl::class.java)
        private const val UI_WEB_ROOT = "swagger-ui"

        /**
         * 'default' is a special case in OpenAPI that does not have a status code.
         */
        private const val DEFAULT_RESPONSE_KEY = "default"
        const val SPECIFICATION_PATH = "/_spec"
        const val COMBINED_SPECIFICATION_PATH = "$SPECIFICATION_PATH/combined.json"

        init {
            addJavaTimeSupport(Json.mapper())
        }
    }

    override fun configureRoutes(router: Router) {
        parseSpecs(router)

        // serve specification and UI
        LOGGER.debug("Adding specification UI at: {}{}", imposterConfig.serverUrl, SPECIFICATION_PATH)
        router.get(COMBINED_SPECIFICATION_PATH).handler(
            resourceService.handleRoute(imposterConfig, configs, vertx) { routingContext: RoutingContext ->
                handleCombinedSpec(routingContext)
            }
        )
        router.getWithRegex("$SPECIFICATION_PATH$").handler(
            resourceService.handleRoute(imposterConfig, configs, vertx) { routingContext: RoutingContext ->
                routingContext.response()
                    .putHeader("Location", "$SPECIFICATION_PATH/")
                    .setStatusCode(HttpUtil.HTTP_MOVED_PERM)
                    .end()
            }
        )
        router.get("$SPECIFICATION_PATH/*").handler(StaticHandler.create(UI_WEB_ROOT))
    }

    private fun parseSpecs(router: Router) {
        val parsedSpecs = mutableListOf<OpenAPI>()

        // specification mock endpoints
        configs.forEach { config: OpenApiPluginConfig ->
            val spec = SpecificationLoader.parseSpecification(config)
            spec.paths.forEach { path: String, pathConfig: PathItem ->
                handlePathOperations(router, config, spec, path, pathConfig)
            }
            parsedSpecs += spec
        }

        allSpecs = parsedSpecs
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the Vert.x router
     * @param config     the plugin configuration
     * @param spec       the OpenAPI specification
     * @param path       the mock path
     * @param pathConfig the path configuration
     */
    private fun handlePathOperations(
        router: Router,
        config: OpenApiPluginConfig,
        spec: OpenAPI,
        path: String,
        pathConfig: PathItem
    ) {
        pathConfig.readOperationsMap().forEach { (httpMethod: PathItem.HttpMethod, operation: Operation) ->
            val fullPath = buildFullPath(buildBasePath(config, spec), path)
            LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath)

            // convert an io.swagger.models.HttpMethod to an io.vertx.core.http.HttpMethod
            val method = HttpMethod.valueOf(httpMethod.name)
            router.route(method, fullPath).handler(buildHandler(config, operation, spec))
        }
    }

    /**
     * Construct the full path from the base path and the operation path.
     *
     * @param basePath          the base path
     * @param specOperationPath the operation path from the OpenAPI specification
     * @return the full path
     */
    private fun buildFullPath(basePath: String, specOperationPath: String): String {
        val operationPath = convertPathToVertx(specOperationPath)
        return if (basePath.endsWith("/")) {
            if (operationPath!!.startsWith("/")) {
                basePath + operationPath.substring(1)
            } else {
                basePath + operationPath
            }
        } else {
            if (operationPath!!.startsWith("/")) {
                basePath + operationPath
            } else {
                "$basePath/$operationPath"
            }
        }
    }

    /**
     * Construct the base path, optionally dependent on the server path,
     * from which the example response will be served.
     *
     * @param config the mock configuration
     * @param spec   the OpenAPI specification
     * @return the base path
     */
    private fun buildBasePath(config: OpenApiPluginConfig, spec: OpenAPI): String {
        if (config.isUseServerPathAsBaseUrl) {
            // Treat the mock server as substitute for 'the' server.
            // Note: OASv2 'basePath' is converted to OASv3 'server' entries.
            spec.servers.firstOrNull()?.let { firstServer ->
                val url = firstServer.url ?: ""
                if (url.length > 1) {
                    // attempt to parse as URI and extract path
                    try {
                        return URI(url).path
                    } catch (ignored: URISyntaxException) {
                    }
                }
            }
        }
        return ""
    }

    /**
     * Returns an OpenAPI specification combining all the given specifications.
     *
     * @param routingContext the Vert.x routing context
     */
    private fun handleCombinedSpec(routingContext: RoutingContext) {
        try {
            routingContext.response()
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(specificationService.getCombinedSpecSerialised(imposterConfig, allSpecs))
        } catch (e: Exception) {
            routingContext.fail(e)
        }
    }

    /**
     * Build a handler for the given operation.
     *
     * @param pluginConfig the plugin configuration
     * @param operation    the specification operation
     * @param spec         the OpenAPI specification
     * @return a route handler
     */
    private fun buildHandler(
        pluginConfig: OpenApiPluginConfig,
        operation: Operation,
        spec: OpenAPI
    ): Handler<RoutingContext> {
        // statically calculate as much as possible
        val statusCodeFactory = buildStatusCodeCalculator(operation)
        return resourceService.handleRoute(imposterConfig, pluginConfig, vertx) { routingContext: RoutingContext ->
            if (!specificationService.isValidRequest(imposterConfig, pluginConfig, routingContext, allSpecs)) {
                return@handleRoute
            }

            val context: MutableMap<String, Any> = mutableMapOf()
            context["operation"] = operation

            val request = routingContext.request()
            val resourceConfig = routingContext.get<ResponseConfigHolder>(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY)
            val defaultBehaviourHandler = Consumer { responseBehaviour: ResponseBehaviour ->

                // set status code regardless of response strategy
                val response = routingContext.response().setStatusCode(responseBehaviour.statusCode)

                findApiResponse(operation, responseBehaviour.statusCode)?.let { specResponse ->
                    if (!responseBehaviour.responseHeaders.containsKey(HttpUtil.CONTENT_TYPE)) {
                        setContentTypeFromSpec(routingContext, responseBehaviour, specResponse)
                    }

                    // build a response from the specification
                    val exampleSender = ResponseSender { rc: RoutingContext, rb: ResponseBehaviour ->
                        exampleService.serveExample(
                            imposterConfig,
                            pluginConfig,
                            rc,
                            rb,
                            specResponse,
                            spec
                        )
                    }

                    // attempt to serve an example from the specification, falling back if not present
                    responseService.sendResponse(
                        pluginConfig,
                        resourceConfig,
                        routingContext,
                        responseBehaviour,
                        exampleSender,
                        ResponseSender { routingContext: RoutingContext, responseBehaviour: ResponseBehaviour ->
                            fallback(
                                routingContext,
                                responseBehaviour
                            )
                        })
                } ?: run {
                    LOGGER.warn(
                        "No response found in specification for {} {} and status code {}",
                        request.method(),
                        request.path(),
                        responseBehaviour.statusCode
                    )
                    response.end()
                }
            }

            scriptHandler(
                pluginConfig,
                resourceConfig,
                routingContext,
                injector,
                context,
                statusCodeFactory,
                openApiResponseBehaviourFactory,
                defaultBehaviourHandler
            )
        }
    }

    private fun setContentTypeFromSpec(
        routingContext: RoutingContext,
        responseBehaviour: ResponseBehaviour,
        optionalResponse: ApiResponse
    ) {
        optionalResponse.content?.let { responseContent: Content ->
            responseContent.keys.firstOrNull()?.let { firstContentType ->
                when (responseContent.size) {
                    0 -> return
                    1 -> LOGGER.debug(
                        "Setting content type [{}] from specification for {}",
                        firstContentType,
                        describeRequestShort(routingContext)
                    )
                    else -> LOGGER.warn(
                        "Multiple content types in specification - selecting first [{}] for {}",
                        firstContentType,
                        describeRequestShort(routingContext)
                    )
                }
                responseBehaviour.responseHeaders[HttpUtil.CONTENT_TYPE] = firstContentType
            }
        }
    }

    private fun buildStatusCodeCalculator(operation: Operation) = StatusCodeFactory { rc: ResponseConfigHolder ->
        rc.responseConfig.statusCode
        // Fall back to the first non-default response for this operation.
        // Note: responses are keyed on their status code and "default" is a
        // special case in OpenAPI that does not have a status code.
            ?: operation.responses?.keys?.firstOrNull { !DEFAULT_RESPONSE_KEY.equals(it, ignoreCase = true) }?.toInt()
            ?: HttpUtil.HTTP_OK
    }

    /**
     * @return an API response for the given [statusCode], or `null`
     */
    private fun findApiResponse(operation: Operation, statusCode: Int): ApiResponse? {
        // openapi statuses are represented as strings
        val status = statusCode.toString()

        // look for a specification response based on the status code
        val optionalResponse = operation.responses?.filter { it.key == status }?.values?.firstOrNull()

        return optionalResponse ?: run {
            // fall back to default (might also be null)
            LOGGER.debug(
                "No response found for status code {}; falling back to default response if present",
                statusCode
            )
            operation.responses.default
        }
    }

    /**
     * Handles the scenario when no example is found.
     *
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private fun fallback(routingContext: RoutingContext, responseBehaviour: ResponseBehaviour): Boolean {
        LOGGER.warn(
            "No example match found and no response file set for mock response for URI {} with status code {}" +
                    " - sending empty response", routingContext.request().absoluteURI(), responseBehaviour.statusCode
        )
        routingContext.response().end()
        return true
    }
}