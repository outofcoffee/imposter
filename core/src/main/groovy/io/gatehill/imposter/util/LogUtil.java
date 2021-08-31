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
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class LogUtil {
    private static final String LOGGER_BASE_PACKAGE = ConfigUtil.CURRENT_PACKAGE;
    private static final String ENV_VAR_LOG_LEVEL = "IMPOSTER_LOG_LEVEL";
    private static final Level DEFAULT_LOG_LEVEL = Level.DEBUG;

    /**
     * Configure the logging level using the value of {@link #ENV_VAR_LOG_LEVEL}, falling back
     * to {@link #DEFAULT_LOG_LEVEL} if empty.
     */
    public static void configureLoggingFromEnvironment() {
        configureLogging(ofNullable(System.getenv(LogUtil.ENV_VAR_LOG_LEVEL)).orElse(DEFAULT_LOG_LEVEL.toString()));
    }

    /**
     * Configure the logging subsystem.
     *
     * @param logLevel the log level
     */
    public static void configureLogging(String logLevel) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final LoggerConfig logger = context.getConfiguration().getLoggerConfig(LOGGER_BASE_PACKAGE);
        logger.setLevel(Level.valueOf(logLevel));
        context.updateLoggers();
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
