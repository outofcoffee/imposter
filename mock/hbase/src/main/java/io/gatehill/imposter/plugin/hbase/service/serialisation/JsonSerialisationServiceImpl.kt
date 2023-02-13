/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.plugin.hbase.service.serialisation

import com.google.common.io.BaseEncoding
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner
import io.gatehill.imposter.plugin.hbase.model.MockScanner
import io.gatehill.imposter.plugin.hbase.model.ResultCell
import io.gatehill.imposter.plugin.hbase.service.ScannerService
import io.gatehill.imposter.util.MapUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.HConstants
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class JsonSerialisationServiceImpl @Inject constructor(
    override val scannerService: ScannerService
) : SerialisationService, DeserialisationService {

    override val logger: Logger = LogManager.getLogger(JsonSerialisationServiceImpl::class.java)

    override fun decodeScanner(httpExchange: HttpExchange): MockScanner {
        return try {
            MapUtil.JSON_MAPPER.readValue(httpExchange.request.body!!.bytes, MockScanner::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun decodeScannerFilterPrefix(scanner: MockScanner): String? {
        if (null != scanner.filter) {
            val filter = JsonObject(scanner.filter)
            if (filter.getString("type").equals("PrefixFilter", ignoreCase = true)) {
                return fromBase64(filter.getString("value"))
            }
        }
        return null
    }

    override fun serialise(tableName: String, recordId: String, result: JsonObject): Buffer {
        val cellSet = JsonObject()
        val rowsJson = JsonArray()
        cellSet.put("Row", rowsJson)
        rowsJson.add(buildRow(result, recordId))
        logger.info("Returning single row with ID: {} for table: {}", recordId, tableName)
        return Buffer.buffer(cellSet.encodePrettily())
    }

    override fun serialise(tableName: String, scannerId: String, results: JsonArray, scanner: InMemoryScanner, rows: Int): Buffer {
        val cellSet = JsonObject()
        val rowsJson = JsonArray()
        cellSet.put("Row", rowsJson)

        // start the row counter from the last position in the scanner
        val lastPosition = scanner.rowCounter.get()
        var i = lastPosition
        while (i < lastPosition + rows && i < results.size()) {
            val result = results.getJsonObject(i)
            rowsJson.add(buildRow(result, buildRowKey(scanner)))
            i++
        }

        // scanner exhausted?
        checkExhausted(tableName, scannerId, results, scanner)
        logger.info("Returning {} rows from scanner {} for table: {}", rowsJson.size(), scannerId, tableName)
        return Buffer.buffer(cellSet.encodePrettily())
    }

    /**
     * Build a JSON HBase row for the given `result`.
     *
     * @param result
     * @param rowKey
     * @return a row
     */
    private fun buildRow(result: JsonObject, rowKey: String?): JsonObject {
        val row = JsonObject()
        row.put("key", toBase64(rowKey))
        val cell = JsonArray()
        row.put("Cell", cell)

        // add cells in sorted order
        buildSortedCells(result).forEach { c: ResultCell ->
            val column = JsonObject()
            cell.add(column)
            column.put("column", toBase64(c.fieldName))
            column.put("timestamp", java.lang.Long.toString(HConstants.LATEST_TIMESTAMP))
            column.put("$", toBase64(c.fieldValue))
            cell.add(column)
        }
        return row
    }

    private fun fromBase64(encoded: String): String {
        return String(BaseEncoding.base64().decode(encoded))
    }

    private fun toBase64(data: String?): String {
        return BaseEncoding.base64().encode(data!!.toByteArray())
    }
}