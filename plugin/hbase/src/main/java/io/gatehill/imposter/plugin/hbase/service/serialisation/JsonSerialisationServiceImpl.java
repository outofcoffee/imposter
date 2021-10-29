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

package io.gatehill.imposter.plugin.hbase.service.serialisation;

import com.google.common.io.BaseEncoding;
import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import io.gatehill.imposter.plugin.hbase.model.MockScanner;
import io.gatehill.imposter.plugin.hbase.service.ScannerService;
import io.gatehill.imposter.util.MapUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.hadoop.hbase.HConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Pete Cornish
 */
public class JsonSerialisationServiceImpl implements SerialisationService, DeserialisationService {
    private static final Logger LOGGER = LogManager.getLogger(JsonSerialisationServiceImpl.class);

    @Inject
    private ScannerService scannerService;

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ScannerService getScannerService() {
        return scannerService;
    }

    @Override
    public MockScanner decodeScanner(RoutingContext routingContext) {
        try {
            return MapUtil.JSON_MAPPER.readValue(routingContext.getBody().getBytes(), MockScanner.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> decodeScannerFilterPrefix(MockScanner scanner) {
        if (null != scanner.getFilter()) {
            JsonObject filter = new JsonObject(scanner.getFilter());
            if (filter.getString("type").equalsIgnoreCase("PrefixFilter")) {
                return Optional.of(fromBase64(filter.getString("value")));
            }
        }

        return Optional.empty();
    }

    @Override
    public Buffer serialise(String tableName, String recordId, JsonObject result) {
        final JsonObject cellSet = new JsonObject();
        final JsonArray rowsJson = new JsonArray();
        cellSet.put("Row", rowsJson);

        rowsJson.add(buildRow(result, recordId));
        LOGGER.info("Returning single row with ID: {} for table: {}", recordId, tableName);

        return Buffer.buffer(cellSet.encodePrettily());
    }

    @Override
    public Buffer serialise(String tableName, String scannerId, JsonArray results, InMemoryScanner scanner, int rows) {
        final JsonObject cellSet = new JsonObject();
        final JsonArray rowsJson = new JsonArray();
        cellSet.put("Row", rowsJson);

        // start the row counter from the last position in the scanner
        final int lastPosition = scanner.getRowCounter().get();
        for (int i = lastPosition; i < (lastPosition + rows) && i < results.size(); i++) {
            final JsonObject result = results.getJsonObject(i);

            rowsJson.add(buildRow(result, buildRowKey(scanner)));
        }

        // scanner exhausted?
        checkExhausted(tableName, scannerId, results, scanner);

        LOGGER.info("Returning {} rows from scanner {} for table: {}", rowsJson.size(), scannerId, tableName);

        return Buffer.buffer(cellSet.encodePrettily());
    }

    /**
     * Build a JSON HBase row for the given {@code result}.
     *
     * @param result
     * @param rowKey
     * @return a row
     */
    private JsonObject buildRow(JsonObject result, String rowKey) {
        final JsonObject row = new JsonObject();

        row.put("key", toBase64(rowKey));

        final JsonArray cell = new JsonArray();
        row.put("Cell", cell);

        // add cells in sorted order
        buildSortedCells(result).forEach(c -> {
            final JsonObject column = new JsonObject();
            cell.add(column);
            column.put("column", toBase64(c.getFieldName()));
            column.put("timestamp", Long.toString(HConstants.LATEST_TIMESTAMP));
            column.put("$", toBase64(c.getFieldValue()));
            cell.add(column);
        });
        return row;
    }

    private String fromBase64(String encoded) {
        return new String(BaseEncoding.base64().decode(encoded));
    }

    private String toBase64(String data) {
        return BaseEncoding.base64().encode(data.getBytes());
    }
}
