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
package io.gatehill.imposter.util

import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.vertx.core.logging.Log4j2LogDelegateFactory
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.LoggerContext
import java.time.OffsetDateTime

/**
 * Common logging functionality.
 *
 * @author Pete Cornish
 */
object LogUtil {
    const val KEY_REQUEST_START = "requestStartNanos"
    const val KEY_SCRIPT_DURATION = "scriptExecutionDuration"

    /**
     * The prefix for script logger names.
     */
    const val LOGGER_SCRIPT_PACKAGE = "script"
    private val LOGGER_PACKAGES = arrayOf(
        ConfigUtil.CURRENT_PACKAGE,
        LOGGER_SCRIPT_PACKAGE
    )
    private const val VERTX_LOGGER_FACTORY = "vertx.logger-delegate-factory-class-name"
    private const val ENV_VAR_LOG_LEVEL = "IMPOSTER_LOG_LEVEL"
    private val DEFAULT_LOG_LEVEL = Level.DEBUG

    private val LOGGER: Logger = LogManager.getLogger(LogUtil::class.java)
    private val statsLogger: Logger = LogManager.getLogger("stats")

    private val shouldLogSummary: Boolean by lazy {
        EnvVars.getEnv("IMPOSTER_LOG_SUMMARY")?.toBoolean() == true
    }

    /**
     * Lowercase list of request header names to log.
     */
    private val requestHeaderNames: Array<String> by lazy {
        EnvVars.getEnv("IMPOSTER_LOG_REQUEST_HEADERS")
            ?.split(",")?.map(String::lowercase)?.toTypedArray()
            ?: emptyArray()
    }

    /**
     * Lowercase list of response header names to log.
     */
    private val responseHeaderNames: Array<String> by lazy {
        EnvVars.getEnv("IMPOSTER_LOG_RESPONSE_HEADERS")
            ?.split(",")?.map(String::lowercase)?.toTypedArray()
            ?: emptyArray()
    }

    /**
     * Whether to log the request body.
     */
    private val logRequestBody: Boolean by lazy {
        EnvVars.getEnv("IMPOSTER_LOG_REQUEST_BODY")?.toBoolean() == true
    }

    /**
     * Whether to log the response body.
     */
    private val logResponseBody: Boolean by lazy {
        EnvVars.getEnv("IMPOSTER_LOG_RESPONSE_BODY")?.toBoolean() == true
    }

    fun configureVertxLogging() {
        // delegate all Vert.x logging to Log4J2
        System.setProperty(VERTX_LOGGER_FACTORY, Log4j2LogDelegateFactory::class.java.canonicalName)
    }

    /**
     * Configure the logging level using the value of [ENV_VAR_LOG_LEVEL], falling back
     * to [DEFAULT_LOG_LEVEL] if empty.
     */
    fun configureLoggingFromEnvironment() {
        configureLogging(EnvVars.getEnv(ENV_VAR_LOG_LEVEL) ?: DEFAULT_LOG_LEVEL.toString())
    }

    /**
     * Configure the logging subsystem.
     *
     * @param logLevel the log level
     */
    fun configureLogging(logLevel: String) {
        val context = LogManager.getContext(false) as LoggerContext
        for (loggerPackage in LOGGER_PACKAGES) {
            val logger = context.configuration.getLoggerConfig(loggerPackage)
            logger.level = Level.valueOf(logLevel)
        }
        context.updateLoggers()
    }

    /**
     * @param httpExchange the HTTP exchange
     * @return a short description of the request
     */
    fun describeRequestShort(httpExchange: HttpExchange): String {
        val request = httpExchange.request
        return request.method.toString() + " " + request.absoluteUri
    }

    /**
     * @param httpExchange the HTTP exchange
     * @param requestId      the request ID (can be null)
     * @return a description of the request
     */
    @JvmOverloads
    fun describeRequest(
        httpExchange: HttpExchange,
        requestId: String? = httpExchange.get(ResourceUtil.RC_REQUEST_ID_KEY)
    ): String {
        val request = httpExchange.request
        val requestIdDescription = requestId?.let { "[$it] " } ?: ""
        return requestIdDescription + request.method + " " + request.absoluteUri
    }

    private fun formatDuration(input: Any) = String.format("%.2f", input)

    fun logCompletion(httpExchange: HttpExchange) {
        if (!shouldLogSummary || !statsLogger.isInfoEnabled) {
            return
        }
        try {
            val request = httpExchange.request
            val response = httpExchange.response

            val fields = mutableMapOf<String, String?>(
                "timestamp" to OffsetDateTime.now().toString(),
                "uri" to request.absoluteUri,
                "path" to request.path,
                "method" to request.method.toString(),
                "statusCode" to response.statusCode.toString(),
            )
            httpExchange.get<Long>(KEY_REQUEST_START)?.let { startNanos ->
                val duration = formatDuration((System.nanoTime() - startNanos) / 1000000f)
                fields["duration"] = duration
            }
            httpExchange.get<Float>(KEY_SCRIPT_DURATION)?.let { scriptDuration ->
                val scriptTime = formatDuration(scriptDuration)
                fields["scriptTime"] = scriptTime
            }
            if (requestHeaderNames.isNotEmpty()) {
                fields += request.headers.filterKeys { requestHeaderNames.contains(it.lowercase()) }
            }
            if (responseHeaderNames.isNotEmpty()) {
                fields += response.getHeadersIgnoreCase(responseHeaderNames)
            }
            if (logRequestBody) {
                fields["requestBody"] = httpExchange.request.bodyAsString
            }
            if (logResponseBody) {
                fields["responseBody"] = httpExchange.response.bodyBuffer?.toString()
            }

            statsLogger.info(MapUtil.STATS_MAPPER.writeValueAsString(fields))

        } catch (e: Exception) {
            LOGGER.trace("Failed to log completion message", e)
        }
    }
}
