package io.gatehill.imposter.util;

import io.vertx.core.MultiMap;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class CollectionUtil {
    private CollectionUtil() {
    }

    /**
     * @return a copy of the input multimap as a {@link Map}
     */
    public static Map<String, String> asMap(MultiMap input) {
        return input.entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @return a copy of the input multimap with all strings in lowercase
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> convertKeysToLowerCase(MultiMap input) {
        return input.entries().stream().collect(
                Collectors.toMap(e -> e.getKey().toLowerCase(), t -> (V) t.getValue())
        );
    }

    /**
     * @return a copy of the input map with all strings in lowercase
     */
    public static <V> Map<String, V> convertKeysToLowerCase(Map<String, V> input) {
        return input.entrySet().stream().collect(
                Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue)
        );
    }
}
