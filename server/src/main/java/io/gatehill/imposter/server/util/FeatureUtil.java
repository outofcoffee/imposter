package io.gatehill.imposter.server.util;

import com.google.inject.Module;
import io.gatehill.imposter.store.StoreModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class FeatureUtil {
    private static final String ENV_IMPOSTER_FEATURES = "IMPOSTER_FEATURES";
    public static final String SYS_PROP_IMPOSTER_FEATURES = "imposter.features";
    private static final Logger LOGGER = LogManager.getLogger(FeatureUtil.class);

    /**
     * Determines which features are enabled by default.
     */
    private static final Map<String, Boolean> DEFAULT_FEATURES = new HashMap<String, Boolean>() {{
        put("metrics", true);
        put("stores", true);
    }};

    /**
     * Maps modules for specific features.
     */
    private static final Map<String, Class<? extends Module>> FEATURE_MODULES = new HashMap<String, Class<? extends Module>>() {{
        put("stores", StoreModule.class);
    }};

    /**
     * Holds the enabled status of the features, keyed by name.
     */
    private static final Map<String, Boolean> FEATURES;

    static {
        final Map<String, Boolean> overrides = listOverrides();
        FEATURES = DEFAULT_FEATURES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                ofNullable(overrides.get(entry.getKey())).orElse(entry.getValue()))
        );

        LOGGER.trace("Features: {}", FEATURES);
    }

    private FeatureUtil() {
    }

    public static Boolean isFeatureEnabled(String featureName) {
        return FEATURES.getOrDefault(featureName, false);
    }

    /**
     * @return a list of {@link Module} instances based on the enabled features
     */
    public static List<Module> discoverFeatureModules() {
        return FEATURE_MODULES.entrySet().stream()
                .filter(entry -> isFeatureEnabled(entry.getKey()))
                .map(entry -> uncheckedInstantiate(entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Converts a string list from environment variable {@link #ENV_IMPOSTER_FEATURES} or
     * system property {@link #SYS_PROP_IMPOSTER_FEATURES} to a {@link Map}.
     * <p>
     * For example:
     * <pre>
     *     "foo=true,bar=false"
     * </pre>
     * <p>
     * becomes:
     * <pre>
     *      [foo: true, bar: false]
     * </pre>
     *
     * @return a map of feature name to enabled status
     */
    private static Map<String, Boolean> listOverrides() {
        final List<String> features = Arrays.asList(ofNullable(System.getenv(ENV_IMPOSTER_FEATURES))
                .orElse(ofNullable(System.getProperty(SYS_PROP_IMPOSTER_FEATURES)).orElse(""))
                .split(","));

        return features.stream()
                .filter(entry -> entry.contains("="))
                .map(entry -> entry.trim().split("="))
                .collect(Collectors.toMap(entry -> entry[0], entry -> Boolean.parseBoolean(entry[1])));
    }

    private static <T> T uncheckedInstantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate: " + clazz.getCanonicalName(), e);
        }
    }
}
