package com.gatehill.imposter;

import io.vertx.core.Launcher;

/**
 * @author pcornish
 */
public class ImposterLauncher extends Launcher {
    private static final String VERTX_LOGGER_FACTORY = "vertx.logger-delegate-factory-class-name";
    private static final String VERTX_LOGGER_IMPL = "io.vertx.core.logging.SLF4JLogDelegateFactory";

    static {
        // delegate all Vert.x logging to SLF4J
        System.setProperty(VERTX_LOGGER_FACTORY, VERTX_LOGGER_IMPL);
    }
}
