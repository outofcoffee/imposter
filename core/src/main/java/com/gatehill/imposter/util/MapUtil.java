package com.gatehill.imposter.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MapUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private MapUtil() {}
}
