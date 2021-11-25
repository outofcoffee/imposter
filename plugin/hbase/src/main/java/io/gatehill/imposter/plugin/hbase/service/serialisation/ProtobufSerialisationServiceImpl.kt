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
package io.gatehill.imposter.plugin.hbase.service.serialisation

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner
import io.gatehill.imposter.plugin.hbase.model.MockScanner
import io.gatehill.imposter.plugin.hbase.model.ResultCell
import io.gatehill.imposter.plugin.hbase.service.ScannerService
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.filter.Filter
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.hadoop.hbase.rest.model.CellModel
import org.apache.hadoop.hbase.rest.model.CellSetModel
import org.apache.hadoop.hbase.rest.model.RowModel
import org.apache.hadoop.hbase.rest.model.ScannerModel
import org.apache.hadoop.hbase.util.Bytes
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ProtobufSerialisationServiceImpl @Inject constructor(
    override val scannerService: ScannerService
) : SerialisationService, DeserialisationService {

    override val logger: Logger = LogManager.getLogger(ProtobufSerialisationServiceImpl::class.java)

    override fun decodeScanner(httpExchange: HttpExchange): MockScanner {
        val scannerModel: ScannerModel
        return try {
            scannerModel = getScannerModel(httpExchange)
            val scanner = MockScanner()
            scanner.filter = scannerModel.filter
            scanner
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun decodeScannerFilterPrefix(scanner: MockScanner): String? {
        val filter: Filter = try {
            ScannerModel.buildFilter(scanner.filter)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        if (filter is PrefixFilter) {
            return Bytes.toString(filter.prefix)
        }
        return null
    }

    /**
     * @param httpExchange
     * @return the scanner
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getScannerModel(httpExchange: HttpExchange): ScannerModel {
        val scannerModel = ScannerModel()
        val rawMessage = httpExchange.body!!.bytes

        // deserialise from protobuf
        scannerModel.getObjectFromMessage(rawMessage)
        return scannerModel
    }

    override fun serialise(tableName: String, recordId: String, result: JsonObject): Buffer {
        val cellSetModel = CellSetModel()
        cellSetModel.addRow(buildRow(result, recordId))
        logger.info("Returning single row with ID: {} for table: {}", recordId, tableName)
        val protobufOutput = cellSetModel.createProtobufOutput()
        return Buffer.buffer(protobufOutput)
    }

    override fun serialise(
        tableName: String,
        scannerId: String,
        results: JsonArray,
        scanner: InMemoryScanner,
        rows: Int
    ): Buffer {
        val cellSetModel = CellSetModel()

        // start the row counter from the last position in the scanner
        val lastPosition = scanner.rowCounter.get()
        var i = lastPosition
        while (i < lastPosition + rows && i < results.size()) {
            val result = results.getJsonObject(i)
            cellSetModel.addRow(buildRow(result, buildRowKey(scanner)))
            i++
        }

        // scanner exhausted?
        checkExhausted(tableName, scannerId, results, scanner)
        logger.info("Returning {} rows from scanner {} for table: {}", cellSetModel.rows.size, scannerId, tableName)
        val protobufOutput = cellSetModel.createProtobufOutput()
        return Buffer.buffer(protobufOutput)
    }

    /**
     * Build an HBase RowModel for the given `result`.
     *
     * @param result
     * @param rowKey
     * @return a row
     */
    private fun buildRow(result: JsonObject, rowKey: String?): RowModel {
        val row = RowModel()
        row.key = Bytes.toBytes(rowKey)

        // add cells in sorted order
        buildSortedCells(result).forEach { c: ResultCell ->
            val cell = CellModel()
            cell.column = Bytes.toBytes(c.fieldName)
            cell.value = Bytes.toBytes(c.fieldValue)
            row.addCell(cell)
        }
        return row
    }
}