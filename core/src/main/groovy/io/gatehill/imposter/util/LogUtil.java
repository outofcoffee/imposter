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

package io.gatehill.imposter.util;

import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import static java.util.Optional.ofNullable;

/**
 * Common logging functionality.
 *
 * @author Pete Cornish
 */
public class LogUtil {
    /**
     * The prefix for script logger names.
     */
    public static final String LOGGER_SCRIPT_PACKAGE = "script";
    private static final String[] LOGGER_PACKAGES = {
        ConfigUtil.CURRENT_PACKAGE,
        LOGGER_SCRIPT_PACKAGE
    };
    private static final String ENV_VAR_LOG_LEVEL = "IMPOSTER_LOG_LEVEL";
    private static final Level DEFAULT_LOG_LEVEL = Level.DEBUG;

    /**
     * Configure the logging level using the value of {@link #ENV_VAR_LOG_LEVEL}, falling back
     * to {@link #DEFAULT_LOG_LEVEL} if empty.
     */
    public static void configureLoggingFromEnvironment() {
        configureLogging(ofNullable(EnvVars.getEnv(LogUtil.ENV_VAR_LOG_LEVEL)).orElse(DEFAULT_LOG_LEVEL.toString()));
    }

    /**
     * Configure the logging subsystem.
     *
     * @param logLevel the log level
     */
    public static void configureLogging(String logLevel) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        for (String loggerPackage : LOGGER_PACKAGES) {
            final LoggerConfig logger = context.getConfiguration().getLoggerConfig(loggerPackage);
            logger.setLevel(Level.valueOf(logLevel));
        }
        context.updateLoggers();
    }

    /**
     * @param routingContext the routing context
     * @return a short description of the request
     */
    public static String describeRequestShort(RoutingContext routingContext) {
        return routingContext.request().method() + " " + routingContext.request().absoluteURI();
    }

    /**
     * @param routingContext the routing context
     * @return a description of the request
     */
    public static String describeRequest(RoutingContext routingContext) {
        return describeRequest(routingContext, routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY));
    }

    /**
     * @param routingContext the routing context
     * @param requestId      the request ID
     * @return a description of the request
     */
    public static String describeRequest(RoutingContext routingContext, String requestId) {
        return "[" + requestId + "]" +
                " " + routingContext.request().method() +
                " " + routingContext.request().absoluteURI();
    }
}
