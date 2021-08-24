package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;

/**
 * Holds the global engine configuration.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ConfigHolder {
    private static ImposterConfig config;

    static {
        resetConfig();
    }

    private ConfigHolder() {
    }

    /**
     * This is primarily used in tests to clear the configuration state.
     */
    public static void resetConfig() {
        config = new ImposterConfig();
    }

    public static ImposterConfig getConfig() {
        return config;
    }
}
