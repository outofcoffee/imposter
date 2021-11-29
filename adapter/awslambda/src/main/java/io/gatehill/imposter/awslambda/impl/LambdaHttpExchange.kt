/*
 * Copyright (c) 2021-2021.
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

package io.gatehill.imposter.awslambda.impl

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.google.common.base.Strings
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.util.HttpUtil
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.charset.Charset

/**
 * @author Pete Cornish
 */
class LambdaHttpExchange(
    private val request: LambdaHttpRequest,
    private val response: LambdaHttpResponse,
    override val currentRoutePath: String?
) : HttpExchange {
    private val attributes = mutableMapOf<String, Any>()
    private var failureCause: Throwable? = null

    private val acceptedMimeTypes: List<String> by lazy {
        HttpUtil.readAcceptedContentTypes(this@LambdaHttpExchange)
    }

    override fun request(): LambdaHttpRequest {
        return request
    }

    override fun response(): LambdaHttpResponse {
        return response
    }

    override fun pathParams(): Map<String, String> {
        return request.event.pathParameters ?: emptyMap()
    }

    override fun queryParams(): Map<String, String> {
        return request.event.queryStringParameters ?: emptyMap()
    }

    override fun pathParam(paramName: String): String? {
        return request.event.pathParameters?.get(paramName)
    }

    override fun queryParam(queryParam: String): String? {
        return request.event.queryStringParameters?.get(queryParam)
    }

    override fun isAcceptHeaderEmpty(): Boolean {
        return Strings.isNullOrEmpty(request.getHeader("Accept"))
    }

    override fun acceptsMimeType(mimeType: String): Boolean {
        // TODO handle wildcard mime types, not just exact matches
        return acceptedMimeTypes.contains(mimeType)
    }

    override val body: Buffer? by lazy {
        request.event.body?.let { Buffer.buffer(it) }
    }

    override val bodyAsString: String
        get() = request.event.body

    override val bodyAsJson: JsonObject? by lazy {
        request.event.body?.let { JsonObject(it) }
    }

    override fun fail(cause: Throwable?) {
        fail(500, cause)
    }

    override fun fail(statusCode: Int) {
        response.setStatusCode(statusCode)
    }

    override fun fail(statusCode: Int, cause: Throwable?) {
        response.setStatusCode(statusCode)
        failureCause = cause
    }

    override fun failure(): Throwable? {
        return failureCause
    }

    override fun <T> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes[key] as T?
    }

    override fun put(key: String, value: Any) {
        attributes[key] = value
    }
}

/**
 * @author Pete Cornish
 */
class LambdaHttpRequest(val event: APIGatewayProxyRequestEvent) : HttpRequest {
    override fun path(): String {
        return event.path ?: ""
    }

    override fun method(): ResourceMethod {
        return ResourceMethod.valueOf(event.httpMethod!!)
    }

    override fun absoluteURI(): String {
        return "http://0.0.0.0${path()}"
    }

    override fun headers(): Map<String, String> {
        return event.headers ?: emptyMap()
    }

    override fun getHeader(headerKey: String): String? {
        return event.headers?.get(headerKey)
    }
}

/**
 * @author Pete Cornish
 */
class LambdaHttpResponse : HttpResponse {
    private var statusCode: Int = 200
    var body: String? = null
    val headers = mutableMapOf<String, String>()

    override fun setStatusCode(statusCode: Int): HttpResponse {
        this.statusCode = statusCode
        return this
    }

    override fun getStatusCode(): Int {
        return this.statusCode
    }

    override fun putHeader(headerKey: String, headerValue: String): HttpResponse {
        headers[headerKey] = headerValue
        return this
    }

    override fun headers(): MultiMap {
        return MultiMap.caseInsensitiveMultiMap().addAll(this.headers)
    }

    override fun sendFile(filePath: String): HttpResponse {
        try {
            end(File(filePath).readText())
        } catch (e: Exception) {
            throw RuntimeException("Error sending file: $filePath", e)
        }
        return this
    }

    override fun end() {
        /* no op */
    }

    override fun end(body: Buffer) {
        end(body.toString(Charset.defaultCharset()))
    }

    override fun end(body: String?) {
        this.body = body
        body?.let {
            if (!headers.containsKey("Content-Length")) {
                headers["Content-Length"] = body.length.toString()
            }
        }
    }
}
