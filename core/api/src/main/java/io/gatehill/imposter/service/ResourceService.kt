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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequestHandler
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.vertx.core.Vertx
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * @author Pete Cornish
 */
interface ResourceService {
    /**
     * Extract the resource configurations from the plugin configuration, if present.
     *
     * @param pluginConfig the plugin configuration
     * @return the resource configurations
     */
    fun resolveResourceConfigs(pluginConfig: PluginConfig): List<ResolvedResourceConfig>

    /**
     * Search for a resource configuration matching the current request.
     *
     * @param resources      the resources from the response configuration
     * @param method         the HTTP method of the current request
     * @param pathTemplate   request path template
     * @param path           the path of the current request
     * @param pathParams     the path parameters of the current request
     * @param queryParams    the query parameters of the current request
     * @param requestHeaders the headers of the current request
     * @param bodySupplier   supplies the request body
     * @return a matching resource configuration or else empty
     */
    fun matchResourceConfig(
        resources: List<ResolvedResourceConfig>,
        method: ResourceMethod,
        pathTemplate: String?,
        path: String?,
        pathParams: Map<String, String>,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
        bodySupplier: Supplier<String?>
    ): ResponseConfigHolder?

    /**
     * Builds a handler that processes a request.
     *
     * If `requestHandlingMode` is [io.gatehill.imposter.server.RequestHandlingMode.SYNC], then the `httpExchangeConsumer`
     * is invoked on the calling thread.
     *
     * If it is [io.gatehill.imposter.server.RequestHandlingMode.ASYNC], then upon receiving a request,
     * the `httpExchangeConsumer` is invoked on a worker thread, passing the `httpExchange`.
     *
     * Example:
     * ```
     * router.get("/example").handler(handleRoute(imposterConfig, allPluginConfigs, vertx, httpExchange -> {
     * // use httpExchange
     * });
     * ```
     *
     * @param imposterConfig         the Imposter configuration
     * @param allPluginConfigs       all plugin configurations
     * @param vertx                  the current Vert.x instance
     * @param httpExchangeConsumer the consumer of the [HttpExchange]
     * @return the handler
     */
    fun handleRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        vertx: Vertx,
        httpExchangeConsumer: Consumer<HttpExchange>
    ): HttpRequestHandler

    /**
     * Builds a handler that processes a request.
     *
     * If `requestHandlingMode` is [io.gatehill.imposter.server.RequestHandlingMode.SYNC], then the `httpExchangeConsumer`
     * is invoked on the calling thread.
     *
     * If it is [io.gatehill.imposter.server.RequestHandlingMode.ASYNC], then upon receiving a request,
     * the `httpExchangeConsumer` is invoked on a worker thread, passing the `httpExchange`.
     *
     * Example:
     * ```
     * router.get("/example").handler(handleRoute(imposterConfig, pluginConfig, vertx, httpExchange -> {
     * // use httpExchange
     * });
     * ```
     *
     * @param imposterConfig         the Imposter configuration
     * @param pluginConfig           the plugin configuration
     * @param vertx                  the current Vert.x instance
     * @param httpExchangeConsumer the consumer of the [HttpExchange]
     * @return the handler
     */
    fun handleRoute(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        vertx: Vertx,
        httpExchangeConsumer: Consumer<HttpExchange>
    ): HttpRequestHandler

    /**
     * Builds a handler that processes a request.
     *
     * If `requestHandlingMode` is [io.gatehill.imposter.server.RequestHandlingMode.SYNC], then the `httpExchangeHandler`
     * is invoked on the calling thread.
     *
     * If it is [io.gatehill.imposter.server.RequestHandlingMode.ASYNC], then upon receiving a request,
     * the `httpExchangeHandler` is invoked on a worker thread, passing the `httpExchange`.
     *
     * Example:
     * ```
     * router.get("/example").handler(handleRoute(imposterConfig, allPluginConfigs, vertx, httpExchangeHandler);
     * ```
     *
     * @param imposterConfig        the Imposter configuration
     * @param allPluginConfigs      all plugin configurations
     * @param vertx                 the current Vert.x instance
     * @param httpExchangeHandler the handler of the [HttpExchange]
     * @return the handler
     */
    fun passthroughRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        vertx: Vertx,
        httpExchangeHandler: HttpRequestHandler
    ): HttpRequestHandler

    /**
     * Catches unhandled exceptions.
     *
     * @return the exception handler
     */
    fun buildUnhandledExceptionHandler(): HttpRequestHandler
}