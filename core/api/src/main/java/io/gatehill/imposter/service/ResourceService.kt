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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpExchangeHandler
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.ResourceMatcher
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.server.ServerFactory

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
     * router.get("/example").handler(handleRoute(imposterConfig, allPluginConfigs, vertx, httpExchange -> {
     * // use httpExchange
     * });
     * ```
     *
     * @param imposterConfig      the Imposter configuration
     * @param allPluginConfigs    all plugin configurations
     * @param resourceMatcher     the [ResourceMatcher] to use
     * @param httpExchangeHandler the consumer of the [HttpExchange]
     * @return the handler
     */
    fun handleRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeFutureHandler,
    ): HttpExchangeFutureHandler

    /**
     * Same as [handleRoute] but wraps [httpExchangeHandler] in a future.
     */
    fun handleRouteAndWrap(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeFutureHandler

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
     * router.get("/example").handler(handleRoute(imposterConfig, pluginConfig, vertx, httpExchange -> {
     * // use httpExchange
     * });
     * ```
     *
     * @param imposterConfig      the Imposter configuration
     * @param pluginConfig        the plugin configuration
     * @param resourceMatcher     the [ResourceMatcher] to use
     * @param httpExchangeHandler the consumer of the [HttpExchange]
     * @return the handler
     */
    fun handleRoute(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeFutureHandler,
    ): HttpExchangeFutureHandler

    /**
     * Same as [handleRoute] but wraps [httpExchangeHandler] in a future.
     */
    fun handleRouteAndWrap(
        imposterConfig: ImposterConfig,
        pluginConfig: PluginConfig,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeHandler,
    ): HttpExchangeFutureHandler

    fun passthroughRoute(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        resourceMatcher: ResourceMatcher,
        httpExchangeHandler: HttpExchangeFutureHandler,
    ): HttpExchangeFutureHandler

    /**
     * Catches 404 responses.
     */
    fun buildNotFoundExceptionHandler(): HttpExchangeHandler

    /**
     * Catches unhandled exceptions.
     *
     * @return the exception handler
     */
    fun buildUnhandledExceptionHandler(): HttpExchangeHandler

    /**
     * Adds handlers for static content.
     */
    fun handleStaticContent(
        serverFactory: ServerFactory,
        allConfigs: List<PluginConfig>,
        router: HttpRouter
    )
}
