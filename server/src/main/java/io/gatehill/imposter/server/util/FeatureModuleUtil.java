package io.gatehill.imposter.server.util;

import com.google.inject.Module;
import io.gatehill.imposter.store.StoreModule;
import io.gatehill.imposter.util.FeatureUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class FeatureModuleUtil {
    /**
     * Maps modules for specific features.
     */
    private static final Map<String, Class<? extends Module>> FEATURE_MODULES = new HashMap<String, Class<? extends Module>>() {{
        put("stores", StoreModule.class);
    }};

    private FeatureModuleUtil() {
    }

    /**
     * @return a list of {@link Module} instances based on the enabled features
     */
    public static List<Module> discoverFeatureModules() {
        return FEATURE_MODULES.entrySet().stream()
                .filter(entry -> FeatureUtil.isFeatureEnabled(entry.getKey()))
                .map(entry -> uncheckedInstantiate(entry.getValue()))
                .collect(Collectors.toList());
    }

    private static <T> T uncheckedInstantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate: " + clazz.getCanonicalName(), e);
        }
    }
}
