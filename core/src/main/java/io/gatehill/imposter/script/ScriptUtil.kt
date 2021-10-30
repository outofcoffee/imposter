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

package io.gatehill.imposter.script

import io.gatehill.imposter.util.CollectionUtil
import io.gatehill.imposter.util.EnvVars
import io.vertx.ext.web.RoutingContext

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish
 */
object ScriptUtil {
    private val forceHeaderKeyNormalisation =
        EnvVars.getEnv("IMPOSTER_NORMALISE_HEADER_KEYS")?.toBoolean() == true

    /**
     * Build the {@code context}, containing lazily-evaluated values.
     *
     * @param routingContext
     * @param additionalContext
     * @return the context
     */
    @JvmStatic
    fun buildContext(routingContext: RoutingContext, additionalContext: Map<String, Any>?): ExecutionContext {
        val vertxRequest = routingContext.request()

        val headersSupplier: () -> Map<String, String> = {
            val entries = vertxRequest.headers()
            if (forceHeaderKeyNormalisation) {
                CollectionUtil.convertKeysToLowerCase(entries)
            } else {
                CollectionUtil.asMap(entries)
            }
        }

        val pathParamsSupplier: () -> Map<String, String> = { routingContext.pathParams() }

        val queryParamsSupplier: () -> Map<String, String> = {
            CollectionUtil.asMap(vertxRequest.params())
        }

        val bodySupplier: () -> String? = { routingContext.bodyAsString }

        // request information
        val request = ExecutionContext.Request(headersSupplier, pathParamsSupplier, queryParamsSupplier, bodySupplier)
        request.path = vertxRequest.path()
        request.method = vertxRequest.method().name
        request.uri = vertxRequest.absoluteURI()

        // root context
        val executionContext = ExecutionContext(request)

        // additional context
        additionalContext?.forEach { executionContext[it.key] = it.value }

        return executionContext
    }
}
