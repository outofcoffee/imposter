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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.model.steps.http.RemoteHttpExchange
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.PlaceholderUtil
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.logging.log4j.LogManager
import java.net.URI
import javax.inject.Inject

/**
 * Handles remote steps.
 *
 * @author Pete Cornish
 */
class RemoteService @Inject constructor(
    private val imposterConfig: ImposterConfig,
) {
    private val logger = LogManager.getLogger(javaClass)
    private val httpClient = OkHttpClient()

    fun sendRequest(
        url: String,
        method: HttpMethod,
        queryParams: Map<String, String>?,
        formParams: Map<String, String>?,
        headers: Map<String, String>?,
        content: String?,
        httpExchange: HttpExchange,
    ): RemoteHttpExchange {
        val call = buildCall(url, method, queryParams, formParams, headers, content, httpExchange)
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
        rawUrl: String,
        method: HttpMethod,
        queryParams: Map<String, String>?,
        formParams: Map<String, String>?,
        headers: Map<String, String>?,
        content: String?,
        httpExchange: HttpExchange,
    ): Call {
        try {
            val url = buildUrl(rawUrl, httpExchange, queryParams)
            logger.info("Sending remote request $method $url")

            val body: RequestBody? = content?.let {
                PlaceholderUtil.replace(it, httpExchange, PlaceholderUtil.templateEvaluators)
            }?.toRequestBody() ?: formParams?.let {
                val formBuilder = FormBody.Builder()
                formParams.forEach { (key, value) ->
                    formBuilder.add(key, value)
                }
                formBuilder.build()
            }

            val requestBuilder = Request.Builder().url(url).method(method.name, body)

            headers?.forEach { (key, rawValue) ->
                val value = PlaceholderUtil.replace(rawValue, httpExchange, PlaceholderUtil.templateEvaluators)
                requestBuilder.header(key, value)
            }

            return httpClient.newCall(requestBuilder.build())

        } catch (e: Exception) {
            throw RuntimeException("Failed to build remote call for ${LogUtil.describeRequest(httpExchange)}", e)
        }
    }

    private fun buildUrl(rawUrl: String, httpExchange: HttpExchange, queryParams: Map<String, String>?): HttpUrl {
        val rawUri = PlaceholderUtil.replace(rawUrl, httpExchange, PlaceholderUtil.templateEvaluators)

        // prefix server URL if URI is relative
        val uri = if (rawUri.startsWith("/")) URI(imposterConfig.serverUrl + rawUri) else URI(rawUri)

        val urlBuilder = HttpUrl.Builder()
                .scheme(uri.scheme)
                .host(uri.host)
                .port(uri.port)
                .addPathSegments(uri.path)
                .query(uri.query)

        queryParams?.forEach { (key, rawValue) ->
            val value = PlaceholderUtil.replace(rawValue, httpExchange, PlaceholderUtil.templateEvaluators)
            urlBuilder.addQueryParameter(key, value)
        }

        return urlBuilder.build()
    }

    private fun handleResponse(
        request: Request,
        response: Response,
        httpExchange: HttpExchange,
    ): RemoteHttpExchange {
        try {
            if (logger.isTraceEnabled) {
                logger.trace("Response from remote URL ${request.url}: $response")
            }

            val body = response.body?.string()
            logger.debug(
                "Received response from remote URL ${request.url} with status ${response.code} [body: ${body?.length} bytes] for ${LogUtil.describeRequest(httpExchange)}"
            )

            return RemoteHttpExchange(httpExchange, request, response, body)

        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to handle response from remote URL ${request.url} for ${LogUtil.describeRequest(httpExchange)}", e
            )
        }
    }
}
