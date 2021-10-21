package io.gatehill.imposter.util;

import java.util.Map;

/**
 * Wrapper for retrieving environment variables, allowing for
 * overrides.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class EnvVars {
    private static EnvVars INSTANCE;

    private final Map<String, String> env;

    static {
        populate(System.getenv());
    }

    public static void populate(Map<String, String> entries) {
        INSTANCE = new EnvVars(entries);
    }

    public EnvVars(Map<String, String> env) {
        this.env = env;
    }

    public static Map<String, String> getEnv() {
        return INSTANCE.env;
    }

    public static String getEnv(String key) {
        return INSTANCE.env.get(key);
    }
}
