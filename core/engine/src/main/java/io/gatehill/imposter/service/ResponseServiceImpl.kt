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
package io.gatehill.imposter.service

import io.gatehill.imposter.exception.ResponseException
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.ContentTypedConfig
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.PlaceholderUtil
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.impl.MimeMapping
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ResponseServiceImpl @Inject constructor(
    private val engineLifecycle: EngineLifecycleHooks,
    private val characteristicsService: CharacteristicsService,
    private val responseFileService: ResponseFileService,
    private val vertx: Vertx,
) : ResponseService {

    private var notFoundMessages = mutableListOf<String>()

    override fun sendEmptyResponse(httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour): Boolean {
        return try {
            LOGGER.debug("Returning empty response for ${describeRequest(httpExchange)}")
            httpExchange.response.end()
            true
        } catch (e: Exception) {
            LOGGER.error("Error sending empty response for ${describeRequest(httpExchange)}", e)
            false
        }
    }

    override fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
    ) {
        sendResponse(
            pluginConfig,
            resourceConfig,
            httpExchange,
            responseBehaviour,
            ResponseSender { rc, rb -> sendEmptyResponse(rc, rb) }
        )
    }

    override fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        vararg fallbackSenders: ResponseSender,
    ) {
        val completion = {
            responseBehaviour.failureType?.let { failureType ->
                characteristicsService.sendFailure(resourceConfig, httpExchange, failureType)
            } ?: run {
                sendResponseInternal(pluginConfig, resourceConfig, httpExchange, responseBehaviour, fallbackSenders)
            }
        }
        val delayMs = characteristicsService.simulatePerformance(responseBehaviour)
        if (delayMs > 0) {
            LOGGER.info("Delaying mock response for {} by {}ms", LogUtil.describeRequestShort(httpExchange), delayMs)
            vertx.setTimer(delayMs.toLong()) { completion() }
        } else {
            completion()
        }
    }

    private fun sendResponseInternal(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        fallbackSenders: Array<out ResponseSender>,
    ) {
        LOGGER.trace(
            "Sending mock response for {} with status code {}",
            LogUtil.describeRequestShort(httpExchange),
            responseBehaviour.statusCode
        )
        finaliseExchange(resourceConfig, httpExchange) {
            try {
                val response = httpExchange.response
                response.setStatusCode(responseBehaviour.statusCode)
                responseBehaviour.responseHeaders.forEach { (name: String?, value: String?) ->
                    val finalValue = if (responseBehaviour.isTemplate) {
                        PlaceholderUtil.replace(value, httpExchange, PlaceholderUtil.templateEvaluators)
                    } else {
                        value
                    }
                    response.putHeader(name, finalValue)
                }
                if (!responseBehaviour.responseFile.isNullOrEmpty()) {
                    responseFileService.serveResponseFile(pluginConfig, resourceConfig, httpExchange, responseBehaviour)

                } else if (!responseBehaviour.content.isNullOrEmpty()) {
                    serveResponseData(resourceConfig, httpExchange, responseBehaviour)

                } else if (resourceConfig is BasicResourceConfig && !resourceConfig.responseConfig.dir.isNullOrEmpty()) {
                    // this should have been caught by the static handler
                    failWithNotFoundResponse(httpExchange, "Request for nonexistent static resource: ${LogUtil.describeRequestShort(httpExchange)}")

                } else {
                    LOGGER.warn("Response file and data are blank for ${describeRequest(httpExchange)}")
                    fallback(httpExchange, responseBehaviour, fallbackSenders)
                }
            } catch (e: Exception) {
                httpExchange.fail(
                    ResponseException(
                        "Error sending mock response with status code ${responseBehaviour.statusCode} for " +
                            describeRequest(httpExchange), e
                    )
                )
            }
        }
    }

    /**
     * Reply with the contents of a String. Content type should be provided, but if not
     * JSON is assumed.
     *
     * @param resourceConfig    the resource configuration
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     */
    private fun serveResponseData(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
    ) {
        LOGGER.info(
            "Serving response data ({} bytes) for {} with status code {}",
            responseBehaviour.content?.length ?: 0,
            LogUtil.describeRequestShort(httpExchange),
            httpExchange.response.statusCode
        )
        // raw data should be considered untrusted as it is not sanitised
        writeResponseData(
            resourceConfig = resourceConfig,
            httpExchange = httpExchange,
            filenameHintForContentType = null,
            origResponseData = Buffer.buffer(responseBehaviour.content),
            template = responseBehaviour.isTemplate,
            trustedData = false
        )
    }

    override fun writeResponseData(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        filenameHintForContentType: String?,
        origResponseData: Buffer,
        template: Boolean,
        trustedData: Boolean,
    ) {
        val response = httpExchange.response
        setContentTypeIfAbsent(resourceConfig, response, filenameHintForContentType)

        val responseData = if (template) {
            resolvePlaceholders(httpExchange, origResponseData)
        } else {
            origResponseData
        }
        response.end(responseData)
    }

    private fun setContentTypeIfAbsent(
        resourceConfig: ResourceConfig?,
        response: HttpResponse,
        filenameHintForContentType: String?,
    ) {
        // explicit content type
        if (resourceConfig is ContentTypedConfig) {
            if (!resourceConfig.contentType.isNullOrEmpty()) {
                response.putHeader(HttpUtil.CONTENT_TYPE, resourceConfig.contentType!!)
            }
        }

        // infer from filename hint
        if (response.getHeader(HttpUtil.CONTENT_TYPE).isNullOrBlank() && !filenameHintForContentType.isNullOrBlank()) {
            val contentType = MimeMapping.getMimeTypeForFilename(filenameHintForContentType)
            if (!contentType.isNullOrEmpty()) {
                LOGGER.debug("Inferred {} content type", contentType)
                response.putHeader(HttpUtil.CONTENT_TYPE, contentType)
            } else {
                // consider something like Tika to probe content type
                LOGGER.debug("Guessing JSON content type")
                response.putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
            }
        }
    }

    private fun resolvePlaceholders(httpExchange: HttpExchange, responseData: Buffer): Buffer {
        if (responseData.length() == 0) {
            return responseData
        }

        val original = responseData.toString(Charsets.UTF_8)
        val evaluated = PlaceholderUtil.replace(original, httpExchange, PlaceholderUtil.templateEvaluators)

        // only rebuffer if changed
        return if (evaluated === original) responseData else Buffer.buffer(evaluated)
    }

    private fun fallback(
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        missingResponseSenders: Array<out ResponseSender>,
    ) {
        for (sender in missingResponseSenders) {
            try {
                if (sender.send(httpExchange, responseBehaviour)) {
                    return
                }
            } catch (e: Exception) {
                LOGGER.warn("Error invoking response sender", e)
            }
        }
        throw ResponseException("All attempts to send a response failed")
    }

    override fun failWithNotFoundResponse(httpExchange: HttpExchange, reason: String) {
        LOGGER.debug("$reason - returning 404 status code")
        httpExchange.put(ResourceUtil.RC_SEND_NOT_FOUND_RESPONSE, true)
        httpExchange.fail(HttpUtil.HTTP_NOT_FOUND)
    }

    override fun sendNotFoundResponse(httpExchange: HttpExchange) = finaliseExchange(null, httpExchange) {
        val response = httpExchange.response
        response.setStatusCode(HttpUtil.HTTP_NOT_FOUND)

        if (httpExchange.acceptsMimeType(HttpUtil.CONTENT_TYPE_HTML)) {
            response.putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_HTML).end(
                """
                |<html>
                |<head><title>Not found</title></head>
                |<body>
                |<h3>Resource not found</h3>
                |<p>
                |No resource exists for: <pre>${httpExchange.request.method} ${httpExchange.request.path}</pre>
                |</p>
                |${notFoundMessages.joinToString("</p>\n<p>", "<p>", "</p>")}
                |<hr/>
                |<p>
                |<em><a href="https://www.imposter.sh">Imposter mock engine</a></em>
                |</p>
                |</body>
                |</html>
                """.trimMargin()
            )
        } else {
            response.putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT).end("Resource not found")
        }
    }

    override fun addNotFoundMessage(message: String) {
        notFoundMessages += message
    }

    override fun finaliseExchange(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        block: () -> Unit,
    ) {
        try {
            block()
        } finally {
            // always set phase and perform tidy up once handled, regardless of outcome
            httpExchange.phase = ExchangePhase.RESPONSE_SENT

            engineLifecycle.forEach { listener: EngineLifecycleListener ->
                listener.afterResponseSent(httpExchange, resourceConfig)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ResponseServiceImpl::class.java)
    }
}
