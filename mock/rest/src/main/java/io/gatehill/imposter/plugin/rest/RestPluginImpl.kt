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
package io.gatehill.imposter.plugin.rest

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.ContentTypedConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.plugin.rest.config.ResourceConfigType
import io.gatehill.imposter.plugin.rest.config.RestPluginConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginResourceConfig
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.util.FileUtil.findRow
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.ResourceUtil.extractResourceMethod
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Plugin for simple RESTful APIs.
 *
 * @author Pete Cornish
 */
@PluginInfo("rest")
class RestPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val responseService: ResponseService,
) : ConfiguredPlugin<RestPluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = RestPluginConfig::class.java

    override fun configureRoutes(router: HttpRouter) {
        configs.forEach { config: RestPluginConfig ->
            // add root handler
            // TODO consider changing this to config.path if non-null
            addObjectHandler(router, "", config, config)

            // add child resource handlers
            config.resources?.forEach { resource: RestPluginResourceConfig ->
                addResourceHandler(router, config, resource)
            }
        }
    }

    private fun addResourceHandler(
        router: HttpRouter,
        rootConfig: RestPluginConfig,
        resourceConfig: RestPluginResourceConfig
    ) {
        val resourceType = resourceConfig.type ?: ResourceConfigType.OBJECT
        when (resourceType) {
            ResourceConfigType.OBJECT -> addObjectHandler(router, rootConfig, resourceConfig)
            ResourceConfigType.ARRAY -> addArrayHandler(router, rootConfig, resourceConfig)
        }
    }

    private fun addObjectHandler(
        router: HttpRouter,
        pluginConfig: RestPluginConfig,
        resourceConfig: ContentTypedConfig
    ) {
        addObjectHandler(router, pluginConfig.path, pluginConfig, resourceConfig)
    }

    private fun addObjectHandler(
        router: HttpRouter,
        rootPath: String?,
        pluginConfig: RestPluginConfig,
        resourceConfig: ContentTypedConfig
    ) {
        val qualifiedPath = buildQualifiedPath(rootPath, resourceConfig)
        val method = extractResourceMethod(resourceConfig)
        LOGGER.debug("Adding {} object handler: {}", method, qualifiedPath)

        router.route(method, qualifiedPath).handler(
            resourceService.handleRoute(imposterConfig, pluginConfig, vertx) { httpExchange: HttpExchange ->
                // script should fire first
                responseService.handle(pluginConfig, resourceConfig, httpExchange) { responseBehaviour ->
                    LOGGER.info(
                        "Handling {} object request for: {}",
                        method,
                        httpExchange.request().absoluteURI()
                    )
                    responseService.sendResponse(pluginConfig, resourceConfig, httpExchange, responseBehaviour)
                }
            }
        )
    }

    private fun addArrayHandler(
        router: HttpRouter,
        pluginConfig: RestPluginConfig,
        resourceConfig: RestPluginResourceConfig
    ) {
        val qualifiedPath = buildQualifiedPath(pluginConfig.path, resourceConfig)
        val method = extractResourceMethod(resourceConfig)
        LOGGER.debug("Adding {} array handler: {}", method, qualifiedPath)

        // validate path includes parameter
        val resourcePath = resourceConfig.path ?: ""
        val matcher = PARAM_MATCHER.matcher(resourcePath)
        require(matcher.matches()) {
            "Resource '$resourcePath' does not contain a field ID parameter"
        }
        router.route(method, qualifiedPath).handler(
            resourceService.handleRoute(imposterConfig, pluginConfig, vertx) { httpExchange: HttpExchange ->
                // script should fire first
                responseService.handle(pluginConfig, resourceConfig, httpExchange) { responseBehaviour ->
                    LOGGER.info("Handling {} array request for: {}", method, httpExchange.request().absoluteURI())

                    // get the first param in the path
                    val idFieldName = matcher.group(1)
                    val idField = httpExchange.pathParam(idFieldName)

                    // find row
                    val result = findRow(
                        idFieldName, idField,
                        responseService.loadResponseAsJsonArray(pluginConfig, responseBehaviour)
                    )
                    val response = httpExchange.response()

                    result?.let {
                        LOGGER.info("Returning single row for {}:{}", idFieldName, idField)
                        response.setStatusCode(HttpUtil.HTTP_OK)
                            .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                            .end(result.encodePrettily())
                    } ?: run {
                        // no such record
                        LOGGER.error("No row found for {}:{}", idFieldName, idField)
                        response.setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
                    }
                }
            })
    }

    private fun buildQualifiedPath(rootPath: String?, resourceConfig: ResourceConfig): String {
        val qualifiedPath = (rootPath ?: "") + (resourceConfig.path ?: "")
        return if (qualifiedPath.startsWith("/")) qualifiedPath else "/$qualifiedPath"
    }

    companion object {
        private val LOGGER = LogManager.getLogger(RestPluginImpl::class.java)

        /**
         * Example: `/anything/:id/something`
         */
        private val PARAM_MATCHER = Pattern.compile(".*:(.+).*")
    }
}
