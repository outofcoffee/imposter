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
package io.gatehill.imposter.service

import com.google.common.base.Strings
import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.exception.ResponseException
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.ContentTypedConfig
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil.describeRequest
import io.gatehill.imposter.util.MetricsUtil
import io.micrometer.core.instrument.Gauge
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.impl.MimeMapping
import io.vertx.core.json.JsonArray
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.readBytes

/**
 * @author Pete Cornish
 */
class ResponseServiceImpl @Inject constructor(
    private val engineLifecycle: EngineLifecycleHooks,
    private val vertx: Vertx,
) : ResponseService {

    /**
     * Holds response files, with maximum number of entries determined by the environment
     * variable [ENV_RESPONSE_FILE_CACHE_ENTRIES].
     */
    private val responseFileCache = CacheBuilder.newBuilder()
        .maximumSize(getEnv(ENV_RESPONSE_FILE_CACHE_ENTRIES)?.toLong() ?: DEFAULT_RESPONSE_FILE_CACHE_ENTRIES)
        .build<Path, Buffer>()

    init {
        MetricsUtil.doIfMetricsEnabled(
            METRIC_RESPONSE_FILE_CACHE_ENTRIES
        ) { registry ->
            Gauge.builder(METRIC_RESPONSE_FILE_CACHE_ENTRIES) { responseFileCache.size() }
                .description("The number of cached response files")
                .register(registry)
        }
    }

    override fun sendEmptyResponse(httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour): Boolean {
        return try {
            LOGGER.info(
                "Response file and data are blank - returning empty response for {}",
                describeRequest(httpExchange)
            )
            httpExchange.response().end()
            true
        } catch (e: Exception) {
            LOGGER.warn("Error sending empty response for " + describeRequest(httpExchange), e)
            false
        }
    }

    override fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour
    ) {
        sendResponse(
            pluginConfig,
            resourceConfig,
            httpExchange,
            responseBehaviour,
            ResponseSender { rc, rb -> sendEmptyResponse(rc, rb) })
    }

    override fun sendResponse(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        vararg fallbackSenders: ResponseSender
    ) {
        simulatePerformance(responseBehaviour, httpExchange.request()) {
            sendResponseInternal(pluginConfig, resourceConfig, httpExchange, responseBehaviour, fallbackSenders)
        }
    }

    private fun simulatePerformance(
        responseBehaviour: ResponseBehaviour,
        request: HttpRequest,
        completion: Runnable
    ) {
        val performance = responseBehaviour.performanceSimulation
        var delayMs = -1
        performance?.let {
            performance.exactDelayMs?.takeIf { it > 0 }?.let { exactDelayMs ->
                delayMs = exactDelayMs
            } ?: run {
                val minDelayMs = performance.minDelayMs ?: 0
                val maxDelayMs = performance.maxDelayMs ?: 0
                if (minDelayMs > 0 && maxDelayMs >= minDelayMs) {
                    delayMs = ThreadLocalRandom.current().nextInt(maxDelayMs - minDelayMs) + minDelayMs
                }
            }
        }
        if (delayMs > 0) {
            LOGGER.info("Delaying mock response for {} {} by {}ms", request.method(), request.absoluteURI(), delayMs)
            vertx.setTimer(delayMs.toLong()) { completion.run() }
        } else {
            completion.run()
        }
    }

    private fun sendResponseInternal(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        fallbackSenders: Array<out ResponseSender>
    ) {
        LOGGER.trace(
            "Sending mock response for URI {} with status code {}",
            httpExchange.request().absoluteURI(),
            responseBehaviour.statusCode
        )
        try {
            val response = httpExchange.response()
            response.setStatusCode(responseBehaviour.statusCode)
            responseBehaviour.responseHeaders.forEach { (name: String?, value: String?) ->
                response.putHeader(name, value)
            }
            if (!Strings.isNullOrEmpty(responseBehaviour.responseFile)) {
                serveResponseFile(pluginConfig, resourceConfig, httpExchange, responseBehaviour)
            } else if (!Strings.isNullOrEmpty(responseBehaviour.responseData)) {
                serveResponseData(resourceConfig, httpExchange, responseBehaviour)
            } else {
                fallback(httpExchange, responseBehaviour, fallbackSenders)
            }
        } catch (e: Exception) {
            httpExchange.fail(
                ResponseException(
                    "Error sending mock response with status code ${responseBehaviour.statusCode} for " +
                            describeRequest(httpExchange), e
                )
            )
        } finally {
            // always set phase and perform tidy up once handled, regardless of outcome
            httpExchange.phase = ExchangePhase.RESPONSE_SENT

            engineLifecycle.forEach { listener: EngineLifecycleListener ->
                listener.afterResponseSent(httpExchange, resourceConfig)
            }
        }
    }

    /**
     * Reply with a static response file. Note that the content type is determined
     * by the file being sent.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     */
    private fun serveResponseFile(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour
    ) {
        val response = httpExchange.response()
        LOGGER.info(
            "Serving response file {} for URI {} with status code {}",
            responseBehaviour.responseFile,
            httpExchange.request().absoluteURI(),
            response.getStatusCode()
        )

        val responseFile = responseBehaviour.responseFile ?: throw IllegalStateException("Response file not set")
        val normalisedPath = normalisePath(pluginConfig, responseFile)

        val responseData = responseFileCache.getIfPresent(normalisedPath) ?: run {
            if (normalisedPath.exists()) {
                Buffer.buffer(normalisedPath.readBytes()).also {
                    responseFileCache.put(normalisedPath, it)
                }
            } else {
                LOGGER.warn("Response file does not exist: $normalisedPath - returning 404 status code")
                httpExchange.response().setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
                return
            }
        }

        writeResponseData(
            resourceConfig = resourceConfig,
            httpExchange = httpExchange,
            filenameHintForContentType = normalisedPath.fileName.toString(),
            origResponseData = responseData,
            template = responseBehaviour.isTemplate,
            trustedData = false
        )
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
        responseBehaviour: ResponseBehaviour
    ) {
        LOGGER.info(
            "Serving response data ({} bytes) for URI {} with status code {}",
            responseBehaviour.responseData!!.length,
            httpExchange.request().absoluteURI(),
            httpExchange.response().getStatusCode()
        )
        // raw data should be considered untrusted as it is not sanitised
        writeResponseData(
            resourceConfig = resourceConfig,
            httpExchange = httpExchange,
            filenameHintForContentType = null,
            origResponseData = Buffer.buffer(responseBehaviour.responseData),
            template = responseBehaviour.isTemplate,
            trustedData = false
        )
    }

    /**
     * Write the response data, optionally resolving placeholders if templating is enabled.
     *
     * @param httpExchange the HTTP exchange
     * @param origResponseData   the data
     */
    private fun writeResponseData(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        filenameHintForContentType: String?,
        origResponseData: Buffer,
        template: Boolean,
        trustedData: Boolean
    ) {
        val response = httpExchange.response()
        setContentTypeIfAbsent(resourceConfig, response, filenameHintForContentType)

        var responseData = origResponseData
        if (template) {
            // listeners may transform response data
            if (!engineLifecycle.isEmpty) {
                engineLifecycle.forEach { listener: EngineLifecycleListener ->
                    responseData = listener.beforeTransmittingTemplate(httpExchange, responseData, trustedData)
                }
            }
        }
        response.end(responseData)
    }

    private fun setContentTypeIfAbsent(
        resourceConfig: ResourceConfig?,
        response: HttpResponse,
        filenameHintForContentType: String?
    ) {
        // explicit content type
        if (resourceConfig is ContentTypedConfig) {
            if (!Strings.isNullOrEmpty(resourceConfig.contentType)) {
                response.putHeader(HttpUtil.CONTENT_TYPE, resourceConfig.contentType!!)
            }
        }

        // infer from filename hint
        if (!response.headers().contains(HttpUtil.CONTENT_TYPE) && !Strings.isNullOrEmpty(filenameHintForContentType)) {
            val contentType = MimeMapping.getMimeTypeForFilename(filenameHintForContentType)
            if (!Strings.isNullOrEmpty(contentType)) {
                LOGGER.debug("Inferred {} content type", contentType)
                response.putHeader(HttpUtil.CONTENT_TYPE, contentType)
            } else {
                // consider something like Tika to probe content type
                LOGGER.debug("Guessing JSON content type")
                response.putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
            }
        }
    }

    private fun fallback(
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        missingResponseSenders: Array<out ResponseSender>
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

    private fun normalisePath(config: PluginConfig, responseFile: String): Path {
        return Paths.get(config.parentDir.absolutePath, responseFile)
    }

    override fun loadResponseAsJsonArray(config: PluginConfig, behaviour: ResponseBehaviour): JsonArray {
        return loadResponseAsJsonArray(config, behaviour.responseFile!!)
    }

    override fun loadResponseAsJsonArray(config: PluginConfig, responseFile: String): JsonArray {
        if (Strings.isNullOrEmpty(responseFile)) {
            LOGGER.debug("Response file blank - returning empty array")
            return JsonArray()
        }
        return try {
            val configPath = normalisePath(config, responseFile).toFile()
            JsonArray(FileUtils.readFileToString(configPath, StandardCharsets.UTF_8))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ResponseServiceImpl::class.java)
        private const val ENV_RESPONSE_FILE_CACHE_ENTRIES = "IMPOSTER_RESPONSE_FILE_CACHE_ENTRIES"
        private const val DEFAULT_RESPONSE_FILE_CACHE_ENTRIES = 20L
        private const val METRIC_RESPONSE_FILE_CACHE_ENTRIES = "response.file.cache.entries"
    }
}
