package io.gatehill.imposter.util;

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
        put(MetricsUtil.FEATURE_NAME_METRICS, true);
        put("stores", true);
    }};

    /**
     * Holds the enabled status of the features, keyed by name.
     */
    private static Map<String, Boolean> FEATURES;

    static {
        refresh();
    }

    private FeatureUtil() {
    }

    public static void refresh() {
        final Map<String, Boolean> overrides = listOverrides();
        FEATURES = DEFAULT_FEATURES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                ofNullable(overrides.get(entry.getKey())).orElse(entry.getValue()))
        );

        LOGGER.trace("Features: {}", FEATURES);
    }

    public static Boolean isFeatureEnabled(String featureName) {
        return FEATURES.getOrDefault(featureName, false);
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
        final List<String> features = Arrays.asList(ofNullable(System.getProperty(SYS_PROP_IMPOSTER_FEATURES, System.getenv(ENV_IMPOSTER_FEATURES))).orElse("")
                .split(","));

        return features.stream()
                .filter(entry -> entry.contains("="))
                .map(entry -> entry.trim().split("="))
                .collect(Collectors.toMap(entry -> entry[0], entry -> Boolean.parseBoolean(entry[1])));
    }

    public static void disableFeature(String featureName) {
        overrideFeature(featureName, false);
    }

    public static void overrideFeature(String featureName, boolean enabled) {
        final Map<String, Boolean> overrides = listOverrides();
        overrides.put(featureName, enabled);

        System.setProperty(SYS_PROP_IMPOSTER_FEATURES, overrides.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")));

        LOGGER.debug("Overriding feature: {}={}", featureName, enabled);
        refresh();
    }

    public static void clearSystemPropertyOverrides() {
        LOGGER.debug("Clearing system property feature overrides");
        System.clearProperty(FeatureUtil.SYS_PROP_IMPOSTER_FEATURES);
        FeatureUtil.refresh();
    }
}
