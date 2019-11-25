package io.gatehill.imposter.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Common logging functionality.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class LogUtil {
    private static final String LOGGER_BASE_PACKAGE = ConfigUtil.CURRENT_PACKAGE;
    public static final String PROPERTY_LOG_LEVEL = LOGGER_BASE_PACKAGE + ".logLevel";

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
}
