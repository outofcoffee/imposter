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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish
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
        final List<String> features = Arrays.asList(ofNullable(System.getProperty(SYS_PROP_IMPOSTER_FEATURES, EnvVars.getEnv(ENV_IMPOSTER_FEATURES))).orElse("")
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
