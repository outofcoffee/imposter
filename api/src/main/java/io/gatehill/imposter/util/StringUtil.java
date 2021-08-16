package io.gatehill.imposter.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class StringUtil {
    private StringUtil() {
    }

    /**
     * Checks if two strings match, where either input could be null.
     *
     * @param a string to test, possibly {@code null}
     * @param b string to test, possibly {@code null}
     * @return {@code true} if the strings match, otherwise {@code false}
     */
    public static boolean safeEquals(String a, String b) {
        if (nonNull(a)) {
            return a.equals(b);
        } else {
            return isNull(b);
        }
    }
}
