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

import com.google.common.base.Strings
import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.MetricsUtil
import io.micrometer.core.instrument.Gauge
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.readBytes

/**
 * @author Pete Cornish
 */
class ResponseFileServiceImpl @Inject constructor(
    private val responseService: ResponseService,
) : ResponseFileService {

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

    override fun serveResponseFile(
        pluginConfig: PluginConfig,
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
    ) {
        val response = httpExchange.response
        LOGGER.info(
            "Serving response file {} for {} with status code {}",
            responseBehaviour.responseFile,
            LogUtil.describeRequestShort(httpExchange),
            response.statusCode
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
                httpExchange.response.setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
                return
            }
        }

        responseService.writeResponseData(
            resourceConfig = resourceConfig,
            httpExchange = httpExchange,
            filenameHintForContentType = normalisedPath.fileName.toString(),
            origResponseData = responseData,
            template = responseBehaviour.isTemplate,
            trustedData = false
        )
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
            JsonArray(configPath.readText())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ResponseFileServiceImpl::class.java)
        private const val ENV_RESPONSE_FILE_CACHE_ENTRIES = "IMPOSTER_RESPONSE_FILE_CACHE_ENTRIES"
        private const val DEFAULT_RESPONSE_FILE_CACHE_ENTRIES = 20L
        private const val METRIC_RESPONSE_FILE_CACHE_ENTRIES = "response.file.cache.entries"
    }
}
