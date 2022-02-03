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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.ResponseBehaviour
import io.vertx.core.json.JsonArray

/**
 * @author Pete Cornish
 */
interface ResponseService {
    fun loadResponseAsJsonArray(config: PluginConfig, behaviour: ResponseBehaviour): JsonArray

    fun loadResponseAsJsonArray(config: PluginConfig, responseFile: String): JsonArray

    /**
     * Send an empty response to the client, typically used as a fallback when no
     * other response can be computed.
     *
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     * @return always `true`
     */
    fun sendEmptyResponse(httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour): Boolean

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, an empty response is returned.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     */
    fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour
    )

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, each of the fallbackSenders is invoked until a response is sent.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     * @param fallbackSenders   the handler(s) to invoke in sequence if a response cannot be computed
     */
    fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        vararg fallbackSenders: ResponseSender
    )

    fun interface ResponseSender {
        @Throws(Exception::class)
        fun send(httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour): Boolean
    }
}
