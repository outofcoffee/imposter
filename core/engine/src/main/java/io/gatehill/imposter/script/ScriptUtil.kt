/*
 * Copyright (c) 2016-2023.
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

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.PluginConfig
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish
 */
object ScriptUtil {
    const val ENV_SCRIPT_CACHE_ENTRIES = "IMPOSTER_SCRIPT_CACHE_ENTRIES"
    const val DEFAULT_SCRIPT_CACHE_ENTRIES = 20L

    val shouldPrecompile = EnvVars.getEnv("IMPOSTER_SCRIPT_PRECOMPILE")?.toBoolean() != false

    private val forceHeaderKeyNormalisation =
        EnvVars.getEnv("IMPOSTER_NORMALISE_HEADER_KEYS")?.toBoolean() != false

    /**
     * Build the {@code context}, containing lazily-evaluated values.
     *
     * @param httpExchange
     * @param additionalContext
     * @return the context
     */
    @JvmStatic
    fun buildContext(httpExchange: HttpExchange, additionalContext: Map<String, Any>?): ExecutionContext {
        val internalRequest = httpExchange.request

        val headersSupplier: () -> Map<String, String> = {
            val entries = internalRequest.headers
            if (forceHeaderKeyNormalisation) {
                LowercaseKeysMap(entries)
            } else {
                entries
            }
        }

        val pathParamsSupplier: () -> Map<String, String> = {
            internalRequest.pathParams
        }
        val queryParamsSupplier: () -> Map<String, String> = {
            internalRequest.queryParams
        }
        val formParamsSupplier: () -> Map<String, String> = {
            internalRequest.formParams
        }
        val bodySupplier: () -> String? = {
            internalRequest.bodyAsString
        }

        // request information
        val request = ExecutionContext.Request(
            headersSupplier,
            pathParamsSupplier,
            queryParamsSupplier,
            formParamsSupplier,
            bodySupplier
        )
        request.path = internalRequest.path
        request.method = internalRequest.method.name
        request.uri = internalRequest.absoluteUri

        // root context
        val executionContext = ExecutionContext(request)

        // additional context
        additionalContext?.forEach { executionContext[it.key] = it.value }

        return executionContext
    }

    fun resolveScriptPath(pluginConfig: PluginConfig, scriptFile: String?): Path =
        Paths.get(pluginConfig.dir.absolutePath, scriptFile!!)
}
