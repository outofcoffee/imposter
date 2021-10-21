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
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import static java.util.Optional.ofNullable

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
final class ScriptUtil {
    private static final Logger LOGGER = LogManager.getLogger(ScriptUtil)
    private final static boolean forceHeaderKeyNormalisation

    static {
        forceHeaderKeyNormalisation = EnvVars.getEnv("IMPOSTER_NORMALISE_HEADER_KEYS")
    }

    /**
     * Build the {@code context}, containing lazily-evaluated values.
     *
     * @param routingContext
     * @param additionalContext
     * @return the context
     */
    static ExecutionContext buildContext(RoutingContext routingContext, Map<String, Object> additionalContext) {
        final vertxRequest = routingContext.request()

        final headersSupplier = { ->
            def entries = vertxRequest.headers().collectEntries()
            return forceHeaderKeyNormalisation ? CollectionUtil.convertKeysToLowerCase(entries) : entries
        }
        final pathParamsSupplier = { -> routingContext.pathParams() }
        final queryParamsSupplier = { -> vertxRequest.params().collectEntries() }
        final bodySupplier = { -> routingContext.getBodyAsString() }

        def deprecatedParams = {
            LOGGER.warn("Deprecation notice: 'context.params' is deprecated and will be removed " +
                    "in a future version. Use 'context.request.queryParams' instead.")
            queryParamsSupplier()
        }

        def deprecatedUri = {
            LOGGER.warn("Deprecation notice: 'context.uri' is deprecated and will be removed " +
                    "in a future version. Use 'context.request.uri' instead.")
            vertxRequest.absoluteURI()
        }

        // root context
        def executionContext = new ExecutionContext()

        // NOTE: params and uri present for legacy script support
        executionContext.metaClass.getParams = deprecatedParams
        executionContext.metaClass.uri = "${-> deprecatedUri()}"

        // request information
        def request = new ExecutionContext.Request(headersSupplier, pathParamsSupplier, queryParamsSupplier, bodySupplier)
        request.path = "${-> vertxRequest.path()}"
        request.method = "${-> vertxRequest.method().name()}"
        request.uri = "${-> vertxRequest.absoluteURI()}"
        executionContext.request = request

        // additional context
        ofNullable(additionalContext).ifPresent({ additional ->
            additional.each { executionContext.metaClass[it.key] = it.value }
        })

        return executionContext
    }
}
