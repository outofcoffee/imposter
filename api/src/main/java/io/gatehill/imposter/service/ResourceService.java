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

package io.gatehill.imposter.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResourceService {
    /**
     * Extract the resource configurations from the plugin configuration, if present.
     *
     * @param pluginConfig the plugin configuration
     * @return the resource configurations
     */
    List<ResolvedResourceConfig> resolveResourceConfigs(PluginConfig pluginConfig);

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
    Optional<ResponseConfigHolder> matchResourceConfig(
            List<ResolvedResourceConfig> resources,
            HttpMethod method,
            String pathTemplate,
            String path,
            Map<String, String> pathParams,
            Map<String, String> queryParams,
            Map<String, String> requestHeaders,
            Supplier<String> bodySupplier
    );

    /**
     * Builds a {@link Handler} that processes a request.
     * <p>
     * If {@code requestHandlingMode} is {@link io.gatehill.imposter.server.RequestHandlingMode#SYNC}, then the {@code routingContextConsumer}
     * is invoked on the calling thread.
     * <p>
     * If it is {@link io.gatehill.imposter.server.RequestHandlingMode#ASYNC}, then upon receiving a request,
     * the {@code routingContextConsumer} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
     *     // use routingContext
     * });
     * </pre>
     *
     * @param imposterConfig         the Imposter configuration
     * @param allPluginConfigs       all plugin configurations
     * @param vertx                  the current Vert.x instance
     * @param routingContextConsumer the consumer of the {@link RoutingContext}
     * @return the handler
     */
    Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            List<? extends PluginConfig> allPluginConfigs,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    );

    /**
     * Builds a {@link Handler} that processes a request.
     * <p>
     * If {@code requestHandlingMode} is {@link io.gatehill.imposter.server.RequestHandlingMode#SYNC}, then the {@code routingContextConsumer}
     * is invoked on the calling thread.
     * <p>
     * If it is {@link io.gatehill.imposter.server.RequestHandlingMode#ASYNC}, then upon receiving a request,
     * the {@code routingContextConsumer} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleRoute(imposterConfig, pluginConfig, vertx, routingContext -> {
     *     // use routingContext
     * });
     * </pre>
     *
     * @param imposterConfig         the Imposter configuration
     * @param pluginConfig           the plugin configuration
     * @param vertx                  the current Vert.x instance
     * @param routingContextConsumer the consumer of the {@link RoutingContext}
     * @return the handler
     */
    Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            PluginConfig pluginConfig,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    );

    /**
     * Builds a {@link Handler} that processes a request.
     * <p>
     * If {@code requestHandlingMode} is {@link io.gatehill.imposter.server.RequestHandlingMode#SYNC}, then the {@code routingContextHandler}
     * is invoked on the calling thread.
     * <p>
     * If it is {@link io.gatehill.imposter.server.RequestHandlingMode#ASYNC}, then upon receiving a request,
     * the {@code routingContextHandler} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleRoute(imposterConfig, allPluginConfigs, vertx, routingContextHandler);
     * </pre>
     *
     * @param imposterConfig        the Imposter configuration
     * @param allPluginConfigs      all plugin configurations
     * @param vertx                 the current Vert.x instance
     * @param routingContextHandler the handler of the {@link RoutingContext}
     * @return the handler
     */
    Handler<RoutingContext> passthroughRoute(
            ImposterConfig imposterConfig,
            List<? extends PluginConfig> allPluginConfigs,
            Vertx vertx,
            Handler<RoutingContext> routingContextHandler
    );

    /**
     * Catches unhandled exceptions.
     *
     * @return the exception handler
     */
    Handler<RoutingContext> buildUnhandledExceptionHandler();
}
