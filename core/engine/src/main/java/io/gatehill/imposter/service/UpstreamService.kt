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

import io.gatehill.imposter.exception.ResponseException
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.resource.PassthroughResourceConfig
import io.gatehill.imposter.plugin.config.resource.UpstreamsHolder
import io.gatehill.imposter.util.LogUtil
import io.vertx.core.buffer.Buffer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.URI
import javax.inject.Inject

/**
 * Proxies requests to an upstream.
 *
 * @author Pete Cornish
 */
class UpstreamService @Inject constructor(
    private val responseService: ResponseService,
) {
    private val logger = LogManager.getLogger(javaClass)
    private val httpClient = OkHttpClient()

    fun forwardToUpstream(
        pluginConfig: UpstreamsHolder,
        resourceConfig: PassthroughResourceConfig,
        httpExchange: HttpExchange,
    ) {
        logger.info("Forwarding request ${LogUtil.describeRequest(httpExchange)} to upstream ${resourceConfig.passthrough}")
        val call = buildCall(pluginConfig, resourceConfig, httpExchange)
        if (logger.isTraceEnabled) {
            logger.trace("Request to upstream ${resourceConfig.passthrough}: ${call.request()}")
        }
        try {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error("Failed to forward request ${LogUtil.describeRequest(httpExchange)} to upstream ${call.request().url}", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    handleResponse(resourceConfig, call.request(), response, httpExchange)
                }
            })
        } catch (e: Exception) {
            logger.error("Failed to forward request ${LogUtil.describeRequest(httpExchange)} to upstream ${call.request().url}", e)
        } finally {
            httpExchange.phase = ExchangePhase.REQUEST_DISPATCHED
        }
    }

    private fun buildCall(pluginConfig: UpstreamsHolder, resourceConfig: PassthroughResourceConfig, httpExchange: HttpExchange): Call {
        try {
            val upstream = pluginConfig.upstreams?.get(resourceConfig.passthrough)
                ?: throw IllegalStateException("No upstream found for name: ${resourceConfig.passthrough}")

            val requestUri = URI(httpExchange.request.absoluteUri)
            val upstreamUri = URI(upstream.url)

            val url = URI(
                upstreamUri.scheme,
                upstreamUri.userInfo ?: requestUri.userInfo,
                upstreamUri.host,
                upstreamUri.port,
                requestUri.path,
                requestUri.query,
                requestUri.fragment
            )

            val request = Request.Builder().url(url.toURL()).apply {
                httpExchange.request.headers.forEach { (name, value) ->
                    if (name !in skipProxyHeaders) {
                        addHeader(name, value)
                    }
                }
            }.method(httpExchange.request.method.name, httpExchange.request.body?.bytes?.toRequestBody()).build()

            return httpClient.newCall(request)

        } catch (e: Exception) {
            throw RuntimeException("Failed to build upstream call for ${LogUtil.describeRequest(httpExchange)}", e)
        }
    }

    private fun handleResponse(
        resourceConfig: PassthroughResourceConfig,
        request: Request,
        response: Response,
        httpExchange: HttpExchange,
    ) {
        try {
            if (logger.isTraceEnabled) {
                logger.trace("Response from upstream ${resourceConfig.passthrough}: $response")
            }

            val body = response.body?.string() ?: ""
            logger.debug(
                "Received response from upstream ${resourceConfig.passthrough} (${request.url}) with status ${response.code} [body: ${body.length} bytes] for ${LogUtil.describeRequest(httpExchange)}"
            )

            with(httpExchange.response) {
                setStatusCode(response.code)
                response.headers.forEach { (name, value) ->
                    if (name !in skipProxyHeaders) {
                        putHeader(name, value)
                    }
                }
            }
            responseService.finaliseExchange(resourceConfig, httpExchange) {
                try {
                    responseService.writeResponseData(
                        resourceConfig,
                        httpExchange,
                        filenameHintForContentType = null,
                        Buffer.buffer(body),
                        template = false,
                        trustedData = false
                    )
                } catch (e: Exception) {
                    httpExchange.fail(
                        ResponseException("Error sending response with status code ${httpExchange.response.statusCode} for ${LogUtil.describeRequest(httpExchange)}", e)
                    )
                }
            }
            LogUtil.logCompletion(httpExchange)

        } catch (e: Exception) {
            logger.error(
                "Failed to handle response from upstream ${resourceConfig.passthrough} (${request.url}) for ${LogUtil.describeRequest(httpExchange)}", e
            )
        }
    }

    companion object {
        val skipProxyHeaders = listOf(
            "Accept-Encoding",
            "Host",

            // Hop-by-hop headers. These are removed in requests to the upstream or responses to the client.
            // See "13.5.1 End-to-end and Hop-by-hop Headers" in http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
            "Connection",
            "Keep-Alive",
            "Proxy-Authenticate",
            "Proxy-Authorization",
            "TE",
            "Trailers",
            "Transfer-Encoding",
            "Upgrade",
        )
    }
}
