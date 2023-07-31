/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.model.steps.http

import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import okhttp3.Request

/**
 * Adapts an OkHttp request to an Imposter request.
 */
class RemoteHttpRequest(private val remoteReq: Request) : HttpRequest {
    override val path: String
        get() = remoteReq.url.encodedPath
    override val method: HttpMethod
        get() = HttpMethod.valueOf(remoteReq.method)
    override val absoluteUri: String
        get() = remoteReq.url.toUri().toString()
    override val headers: Map<String, String>
        get() = remoteReq.headers.toMap()

    override fun getHeader(headerKey: String): String? {
        return remoteReq.header(headerKey)
    }

    override val pathParams: Map<String, String>
        get() = throw UnsupportedOperationException()

    override fun getPathParam(paramName: String): String? {
        throw UnsupportedOperationException()
    }

    override val queryParams: Map<String, String>
        get() = throw UnsupportedOperationException()

    override fun getQueryParam(queryParam: String): String? {
        throw UnsupportedOperationException()
    }

    override val formParams: Map<String, String>
        get() = throw UnsupportedOperationException()

    override fun getFormParam(formParam: String): String? {
        throw UnsupportedOperationException()
    }

    override val body: Buffer?
        get() = throw UnsupportedOperationException()
    override val bodyAsString: String?
        get() = throw UnsupportedOperationException()
    override val bodyAsJson: JsonObject?
        get() = throw UnsupportedOperationException()
}
