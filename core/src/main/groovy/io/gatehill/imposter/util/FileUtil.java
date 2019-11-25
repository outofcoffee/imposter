package io.gatehill.imposter.util;

import com.google.common.base.Strings;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class FileUtil {
    public static final String CLASSPATH_PREFIX = "classpath:";

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
