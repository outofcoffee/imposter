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
package io.gatehill.imposter.lifecycle

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ExecutionContext

/**
 * Hooks for engine lifecycle events.
 *
 * @author Pete Cornish
 */
interface EngineLifecycleListener {
    /**
     * Invoked after inbuilt and plugin routes have been configured.
     *
     * @param imposterConfig   the Imposter configuration
     * @param allPluginConfigs all plugin configurations
     * @param router           the router
     */
    fun afterRoutesConfigured(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        router: HttpRouter
    ) {
        // no op
    }

    /**
     * Invoked before building the script runtime context.
     *
     * @param httpExchange the HTTP exchange
     * @param additionalBindings the additional bindings that will be passed to the script
     * @param executionContext   the script execution context
     */
    fun beforeBuildingRuntimeContext(
        httpExchange: HttpExchange,
        additionalBindings: MutableMap<String, Any>,
        executionContext: ExecutionContext
    ) {
        // no op
    }

    /**
     * Invoked before sending response content when templating is enabled for the active resource.
     *
     * @param httpExchange the HTTP exchange
     * @param responseTemplate the response content
     * @param trustedData whether the response content is trusted/untainted
     * @return the transformed response content
     */
    fun beforeTransmittingTemplate(httpExchange: HttpExchange, responseTemplate: String?, trustedData: Boolean): String? {
        // no op
        return responseTemplate
    }

    /**
     * Invoked before building the response.
     *
     * @param httpExchange the HTTP exchange
     * @param resourceConfig the active resource
     */
    fun beforeBuildingResponse(httpExchange: HttpExchange, resourceConfig: ResponseConfigHolder?) {
        // no op
    }

    /**
     * Invoked after the HTTP exchange handler has returned. This assumes that the handler has blocked
     * until required processing is complete, and that it is safe to perform cleanup activities.
     *
     * @param httpExchange the HTTP exchange
     */
    fun afterHttpExchangeHandled(httpExchange: HttpExchange) {
        // no op
    }
}