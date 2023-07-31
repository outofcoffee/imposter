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

package io.gatehill.imposter.service

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.util.LogUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.logging.log4j.LogManager
import java.net.URI

/**
 * Handles remote steps.
 *
 * @author Pete Cornish
 */
class RemoteService {
    private val logger = LogManager.getLogger(javaClass)
    private val httpClient = OkHttpClient()

    fun sendRequest(
        url: String,
        method: HttpMethod,
        content: String?,
        httpExchange: HttpExchange,
    ): HttpExchange {
        logger.info("Sending request ${LogUtil.describeRequest(httpExchange)} to remote URL $url")
        val call = buildCall(url, method, content, httpExchange)
        if (logger.isTraceEnabled) {
            logger.trace("Request to remote: ${call.request()}")
        }
        try {
            val response = call.execute()
            return handleResponse(call.request(), response, httpExchange)
        } catch (e: Exception) {
            throw RuntimeException("Failed to send request ${LogUtil.describeRequest(httpExchange)} to remote ${call.request().url}", e)
        }
    }

    private fun buildCall(
        url: String,
        method: HttpMethod,
        content: String?,
        httpExchange: HttpExchange,
    ): Call {
        try {
            val request = Request.Builder()
                .url(URI(url).toURL())
                .method(method.name, content?.toRequestBody())
                .build()

            return httpClient.newCall(request)

        } catch (e: Exception) {
            throw RuntimeException("Failed to build remote call for ${LogUtil.describeRequest(httpExchange)}", e)
        }
    }

    private fun handleResponse(
        request: Request,
        response: Response,
        httpExchange: HttpExchange,
    ): HttpExchange {
        try {
            if (logger.isTraceEnabled) {
                logger.trace("Response from remote URL ${request.url}: $response")
            }

            val body = response.body?.string()
            logger.debug(
                "Received response from remote URL ${request.url} with status ${response.code} [body: ${body?.length} bytes] for ${LogUtil.describeRequest(httpExchange)}"
            )

            return adaptRemoteExchange(httpExchange, request, response, body)

        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to handle response from remote URL ${request.url} for ${LogUtil.describeRequest(httpExchange)}", e
            )
        }
    }

    private fun adaptRemoteExchange(initiatingExchange: HttpExchange, remoteReq: Request, remoteResp: Response, remoteRespBody: String?): HttpExchange {
        return object : HttpExchange {
            override var phase = ExchangePhase.RESPONSE_SENT

            override val request = object: HttpRequest {
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

            override val response: HttpResponse
                get() = object : HttpResponse {
                    override fun setStatusCode(statusCode: Int): HttpResponse {
                        throw UnsupportedOperationException()
                    }

                    override val statusCode: Int
                        get() = remoteResp.code

                    override fun putHeader(headerKey: String, headerValue: String): HttpResponse {
                        throw UnsupportedOperationException()
                    }

                    override fun getHeader(headerKey: String): String? {
                        return remoteResp.header(headerKey)
                    }

                    override fun getHeadersIgnoreCase(headerKeys: Array<String>): Map<String, String> {
                        return remoteResp.headers.toMap().mapKeys { (key, _) -> key.lowercase() }
                    }

                    override fun end() {
                        throw UnsupportedOperationException()
                    }

                    override fun end(body: Buffer) {
                        throw UnsupportedOperationException()
                    }

                    override fun close() {
                        throw UnsupportedOperationException()
                    }

                    override val bodyBuffer: Buffer?
                        get() = remoteRespBody?.let { Buffer.buffer(remoteRespBody) }

                    override var finished: Boolean
                        get() = true
                        set(_) {
                            throw UnsupportedOperationException()
                        }
                }

            override fun isAcceptHeaderEmpty(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun acceptsMimeType(mimeType: String): Boolean {
                throw UnsupportedOperationException()
            }

            override val currentRoute: HttpRoute?
                get() = throw UnsupportedOperationException()

            override fun fail(cause: Throwable?) {
                throw UnsupportedOperationException()
            }

            override fun fail(statusCode: Int) {
                throw UnsupportedOperationException()
            }

            override fun fail(statusCode: Int, cause: Throwable?) {
                throw UnsupportedOperationException()
            }

            override fun failure(): Throwable? {
                throw UnsupportedOperationException()
            }

            override fun <T> get(key: String): T? {
                return initiatingExchange.get<T>(key)
            }

            override fun put(key: String, value: Any) {
                initiatingExchange.put(key, value)
            }
        }
    }
}
