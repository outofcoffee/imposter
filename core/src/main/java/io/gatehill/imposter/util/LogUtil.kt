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
package io.gatehill.imposter.util

import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext

/**
 * Common logging functionality.
 *
 * @author Pete Cornish
 */
object LogUtil {
    /**
     * The prefix for script logger names.
     */
    const val LOGGER_SCRIPT_PACKAGE = "script"
    private val LOGGER_PACKAGES = arrayOf(
        ConfigUtil.CURRENT_PACKAGE,
        LOGGER_SCRIPT_PACKAGE
    )
    private const val ENV_VAR_LOG_LEVEL = "IMPOSTER_LOG_LEVEL"
    private val DEFAULT_LOG_LEVEL = Level.DEBUG

    /**
     * Configure the logging level using the value of [.ENV_VAR_LOG_LEVEL], falling back
     * to [.DEFAULT_LOG_LEVEL] if empty.
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
     * @param routingContext the routing context
     * @return a short description of the request
     */
    @JvmStatic
    fun describeRequestShort(routingContext: RoutingContext): String {
        return routingContext.request().method().toString() + " " + routingContext.request().absoluteURI()
    }

    /**
     * @param routingContext the routing context
     * @param requestId      the request ID (can be null)
     * @return a description of the request
     */
    @JvmOverloads
    fun describeRequest(
        routingContext: RoutingContext,
        requestId: String? = routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY)
    ): String {
        return "[" + requestId + "]" +
            " " + routingContext.request().method() +
            " " + routingContext.request().absoluteURI()
    }
}