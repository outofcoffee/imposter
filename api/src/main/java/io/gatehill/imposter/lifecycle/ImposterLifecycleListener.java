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

package io.gatehill.imposter.lifecycle;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;

/**
 * Hooks for engine lifecycle events.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ImposterLifecycleListener {
    /**
     * Invoked after inbuilt and plugin routes have been configured.
     *
     * @param imposterConfig   the Imposter configuration
     * @param allPluginConfigs all plugin configurations
     * @param router           the router
     */
    default void afterRoutesConfigured(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs, Router router) {
        // no op
    }

    /**
     * Invoked on each request to determine if the request is permitted to proceed.
     *
     * @param rootResourceConfig      the root resource configuration
     * @param resourceConfig          the resource configuration for this request
     * @param resolvedResourceConfigs the resolved resource configurations
     * @param routingContext          the routing context
     * @return {@code true} if the request is permitted, otherwise {@code false}
     */
    default boolean isRequestPermitted(
            ResponseConfigHolder rootResourceConfig,
            ResponseConfigHolder resourceConfig,
            List<ResolvedResourceConfig> resolvedResourceConfigs,
            RoutingContext routingContext
    ) {
        return true;
    }

    /**
     * Invoked before building the script runtime context.
     *
     * @param routingContext the routing context
     * @param additionalBindings the additional bindings that will be passed to the script
     * @param executionContext   the script execution context
     */
    default void beforeBuildingRuntimeContext(RoutingContext routingContext, Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        // no op
    }

    /**
     * Invoked following successful execution of the script.
     *
     * @param additionalBindings the additional bindings that were passed to the script
     * @param responseBehaviour  the result of the script execution
     */
    default void afterSuccessfulScriptExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour) {
        // no op
    }

    /**
     * Invoked before sending response content when templating is enabled for the active resource.
     *
     * @param routingContext the routing context
     * @param responseTemplate the response content
     * @return the transformed response content
     */
    default String beforeTransmittingTemplate(RoutingContext routingContext, String responseTemplate) {
        // no op
        return responseTemplate;
    }

    /**
     * Invoked before building the response.
     *
     * @param routingContext the routing context
     * @param resourceConfig the active resource
     */
    default void beforeBuildingResponse(RoutingContext routingContext, ResponseConfigHolder resourceConfig) {
        // no op
    }

    /**
     * Invoked after the routing context handler has returned. This assumes that the handler has blocked
     * until required processing is complete, and that it is safe to perform cleanup activities.
     *
     * @param routingContext the routing context
     */
    default void afterRoutingContextHandled(RoutingContext routingContext) {
        // no op
    }
}
