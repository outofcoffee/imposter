package io.gatehill.imposter.server.util;

import io.gatehill.imposter.ImposterConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ConfigUtil {
    private static ImposterConfig config;

    static {
        resetConfig();
    }

    private ConfigUtil() {
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
