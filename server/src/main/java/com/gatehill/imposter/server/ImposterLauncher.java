package com.gatehill.imposter.server;

import com.gatehill.imposter.util.LogUtil;
import io.vertx.core.Launcher;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterLauncher extends Launcher {
    private static final String VERTX_LOGGER_FACTORY = "vertx.logger-delegate-factory-class-name";
    private static final String VERTX_LOGGER_IMPL = "io.vertx.core.logging.SLF4JLogDelegateFactory";

    static {
        // delegate all Vert.x logging to SLF4J
        System.setProperty(VERTX_LOGGER_FACTORY, VERTX_LOGGER_IMPL);
    }

    /**
     * Main entry point.
     *
     * @param args the user command line arguments.
     */
    public static void main(String[] args) {
        LogUtil.configureLogging(System.getProperty(LogUtil.PROPERTY_LOG_LEVEL, "DEBUG"));

        new ImposterLauncher().dispatch(args);
    }
}
