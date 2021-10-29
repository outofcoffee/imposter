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
import java.util.function.Supplier

/**
 * Representation of the request, supporting lazily-initialised collections for params and headers.
 *
 * @author Pete Cornish
 */
class ExecutionContext {
    var request: Request? = null

    override fun toString(): String {
        return "ExecutionContext{" +
                "request=" + request +
                '}'
    }

    class Request(
        private val headersSupplier: Supplier<Map<String, String>>,
        private val pathParamsSupplier: Supplier<Map<String, String>>,
        private val queryParamsSupplier: Supplier<Map<String, String>>,
        private val bodySupplier: Supplier<String>,
    ) {
        lateinit var path: String
        lateinit var method: String
        lateinit var uri: String

        val headers: Map<String, String> by lazy {
            headersSupplier.get()
        }

        /**
         * @return the request path parameters
         */
        val pathParams: Map<String, String> by lazy {
            pathParamsSupplier.get()
        }

        /**
         * @return the request query parameters
         */
        val queryParams: Map<String, String> by lazy {
            queryParamsSupplier.get()
        }

        /**
         * @return the request body
         */
        val body: String? by lazy {
            bodySupplier.get()
        }

        /**
         * @return the [.getHeaders] map, but with all keys in lowercase
         */
        val normalisedHeaders: Map<String, String>
            get() = CollectionUtil.convertKeysToLowerCase(headers)

        /**
         * Use [.getQueryParams] instead.
         *
         * @return the request query parameters
         */
        @get:Deprecated("")
        val params: Map<String, String>
            get() = queryParams

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
}