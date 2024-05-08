/*
 * Copyright (c) 2024.
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

package io.gatehill.imposter.model.script

import io.gatehill.imposter.script.ScriptRequest
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptRequestBuilder
import io.gatehill.imposter.util.CollectionUtil
import java.util.function.Supplier

/**
 * Representation of the request, supporting lazily-initialised collections for params and headers.
 */
class LazyScriptRequest(
    override val path: String,
    override val method: String,
    override val uri: String,
    private val headersSupplier: Supplier<Map<String, String>>,
    private val pathParamsSupplier: Supplier<Map<String, String>>,
    private val queryParamsSupplier: Supplier<Map<String, String>>,
    private val formParamsSupplier: Supplier<Map<String, String>>,
    private val bodySupplier: Supplier<String?>,
) : ScriptRequest {
    override val headers: Map<String, String> by lazy {
        headersSupplier.get()
    }

    /**
     * @return the request path parameters
     */
    override val pathParams: Map<String, String>
        get() {
            return pathParamsSupplier.get()
        }

    /**
     * @return the request query parameters
     */
    override val queryParams: Map<String, String> by lazy {
        queryParamsSupplier.get()
    }

    /**
     * @return the request form parameters
     */
    override val formParams: Map<String, String> by lazy {
        formParamsSupplier.get()
    }

    /**
     * @return the request body
     */
    override val body: String? by lazy {
        bodySupplier.get()
    }

    /**
     * @return the [headers] map, but with all keys in lowercase
     */
    override val normalisedHeaders: Map<String, String>
        get() = CollectionUtil.convertKeysToLowerCase(headers)

    /**
     * Legacy property removed.
     */
    @get:Deprecated("Use queryParams instead.", ReplaceWith("queryParams"))
    override val params: Map<String, String>
        get() = throw UnsupportedOperationException(
            "Error: the deprecated 'context.request.params' property was removed. Use 'context.request.queryParams' or 'context.request.pathParams' instead."
        )

    override fun toString(): String {
        return "Request{" +
            "path='" + path + '\'' +
            ", method='" + method + '\'' +
            ", uri='" + uri + '\'' +
            ", pathParams=" + pathParams +
            ", queryParams=" + queryParams +
            ", headers=" + headers +
            ", body=<" + (body?.let { "${it.length} bytes" } ?: "null") + '>' +
            '}'
    }
}

/**
 * Constructs a [LazyScriptRequest].
 */
val lazyScriptRequestBuilder : ScriptRequestBuilder = { request ->
    val headersSupplier: () -> Map<String, String> = {
        ScriptUtil.caseHeaders(request)
    }

    val pathParamsSupplier: () -> Map<String, String> = {
        request.pathParams
    }
    val queryParamsSupplier: () -> Map<String, String> = {
        request.queryParams
    }
    val formParamsSupplier: () -> Map<String, String> = {
        request.formParams
    }
    val bodySupplier: () -> String? = {
        request.bodyAsString
    }

    LazyScriptRequest(
        path = request.path,
        method = request.method.name,
        uri = request.absoluteUri,
        headersSupplier,
        pathParamsSupplier,
        queryParamsSupplier,
        formParamsSupplier,
        bodySupplier
    )
}
