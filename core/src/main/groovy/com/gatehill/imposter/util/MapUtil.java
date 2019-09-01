package com.gatehill.imposter.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MapUtil {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final ObjectMapper YAML_MAPPER = new YAMLMapper();

    static {
        JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JSON_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private MapUtil() {}
}
