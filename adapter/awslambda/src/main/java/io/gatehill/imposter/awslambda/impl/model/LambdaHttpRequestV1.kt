/*
 * Copyright (c) 2022-2023.
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

package io.gatehill.imposter.awslambda.impl.model

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.google.common.io.BaseEncoding
import io.gatehill.imposter.awslambda.util.FormParserUtil
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.script.LowercaseKeysMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

/**
 * @author Pete Cornish
 */
class LambdaHttpRequestV1(
    private val event: APIGatewayProxyRequestEvent,
    private val currentRoute: HttpRoute?,
) : HttpRequest {
    private val baseUrl: String

    private val pathParameters by lazy {
        currentRoute?.extractPathParams(path) ?: emptyMap()
    }

    private val _headers by lazy {
        event.headers?.let { LowercaseKeysMap(it) } ?: emptyMap()
    }

    init {
        baseUrl = "http://" + (getHeader("Host") ?: "0.0.0.0")
    }

    override val path: String
        get() = event.path ?: ""

    override val method: HttpMethod
        get() = HttpMethod.valueOf(event.httpMethod!!)

    override val absoluteUri: String
        get() = "$baseUrl$path"

    override val headers: Map<String, String>
        get() = _headers

    override fun getHeader(headerKey: String): String? {
        return headers[headerKey]
    }

    override val pathParams: Map<String, String>
        get() = pathParameters

    override fun getPathParam(paramName: String): String? {
        return pathParameters[paramName]
    }

    override val queryParams: Map<String, String>
        get() = event.queryStringParameters ?: emptyMap()

    override fun getQueryParam(queryParam: String): String? {
        return event.queryStringParameters?.get(queryParam)
    }

    override val formParams: Map<String, String>
        get() = FormParserUtil.getAll(this)

    override fun getFormParam(formParam: String): String? {
        return FormParserUtil.getParam(this, formParam)
    }

    /**
     * Holds the request body, decoding it from Base-64 if required.
     */
    private val requestBodyDecoded: String? by lazy {
        return@lazy event.body?.let { rawBody ->
            if (event.isBase64Encoded) String(BaseEncoding.base64().decode(rawBody)) else rawBody
        }
    }

    override val body: Buffer? by lazy {
        requestBodyDecoded?.let { Buffer.buffer(it) }
    }

    override val bodyAsString: String?
        get() = requestBodyDecoded

    override val bodyAsJson: JsonObject? by lazy {
        requestBodyDecoded?.let { JsonObject(it) }
    }
}
