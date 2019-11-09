package com.gatehill.imposter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static com.gatehill.imposter.util.MapUtil.YAML_MAPPER;
import static java.util.Optional.*;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class FileUtil {
    public static final String CLASSPATH_PREFIX = "classpath:";

    static final String CONFIG_FILE_SUFFIX = "-config";

    static final Map<String, ObjectMapper> CONFIG_FILE_MAPPERS = new HashMap<String, ObjectMapper>() {{
        put(".json", JSON_MAPPER);
        put(".yaml", YAML_MAPPER);
        put(".yml", YAML_MAPPER);
    }};

    private FileUtil() {
    }

    /**
     * Return the row with the given ID.
     *
     * @param idFieldName
     * @param rowId
     * @param rows
     * @return
     */
    public static Optional<JsonObject> findRow(String idFieldName, String rowId, JsonArray rows) {
        if (Strings.isNullOrEmpty(idFieldName)) {
            throw new IllegalStateException("ID field name not configured");
        }

        for (int i = 0; i < rows.size(); i++) {
            final JsonObject row = rows.getJsonObject(i);
            if (ofNullable(row.getValue(idFieldName).toString()).orElse("").equalsIgnoreCase(rowId)) {
                return of(row);
            }
        }
        return empty();
    }
}
