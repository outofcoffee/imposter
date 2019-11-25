package io.gatehill.imposter.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.util.Arrays;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MapUtil {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private static final ObjectMapper[] DESERIALISERS = {
            JSON_MAPPER,
            YAML_MAPPER
    };

    static {
        JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JSON_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        Arrays.stream(DESERIALISERS)
                .forEach(mapper -> mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    private MapUtil() {
    }
}
