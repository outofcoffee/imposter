package io.gatehill.imposter.plugin.hbase.service.serialisation;

import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import io.gatehill.imposter.plugin.hbase.model.MockScanner;
import io.gatehill.imposter.plugin.hbase.service.ScannerService;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.rest.model.CellModel;
import org.apache.hadoop.hbase.rest.model.CellSetModel;
import org.apache.hadoop.hbase.rest.model.RowModel;
import org.apache.hadoop.hbase.rest.model.ScannerModel;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.empty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ProtobufSerialisationServiceImpl implements SerialisationService, DeserialisationService {
    private static final Logger LOGGER = LogManager.getLogger(ProtobufSerialisationServiceImpl.class);

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
        final ScannerModel scannerModel;
        try {
            scannerModel = getScannerModel(routingContext);

            final MockScanner scanner = new MockScanner();
            scanner.setFilter(scannerModel.getFilter());
            return scanner;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> decodeScannerFilterPrefix(MockScanner scanner) {
        final Filter filter;
        try {
            filter = ScannerModel.buildFilter(scanner.getFilter());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (filter instanceof PrefixFilter) {
            final String prefix = Bytes.toString(((PrefixFilter) filter).getPrefix());
            return Optional.of(prefix);
        }
        return empty();
    }

    /**
     * @param routingContext
     * @return the scanner
     * @throws IOException
     */
    private ScannerModel getScannerModel(RoutingContext routingContext) throws IOException {
        final ScannerModel scannerModel = new ScannerModel();
        final byte[] rawMessage = routingContext.getBody().getBytes();

        // deserialise from protobuf
        scannerModel.getObjectFromMessage(rawMessage);

        return scannerModel;
    }

    @Override
    public Buffer serialise(String tableName, String recordId, JsonObject result) {
        final CellSetModel cellSetModel = new CellSetModel();
        cellSetModel.addRow(buildRow(result, recordId));

        LOGGER.info("Returning single row with ID: {} for table: {}", recordId, tableName);

        final byte[] protobufOutput = cellSetModel.createProtobufOutput();

        return Buffer.buffer(protobufOutput);
    }

    @Override
    public Buffer serialise(String tableName, String scannerId, JsonArray results, InMemoryScanner scanner, int rows) {
        final CellSetModel cellSetModel = new CellSetModel();

        // start the row counter from the last position in the scanner
        final int lastPosition = scanner.getRowCounter().get();
        for (int i = lastPosition; i < (lastPosition + rows) && i < results.size(); i++) {
            final JsonObject result = results.getJsonObject(i);

            cellSetModel.addRow(buildRow(result, buildRowKey(scanner)));
        }

        // scanner exhausted?
        checkExhausted(tableName, scannerId, results, scanner);

        LOGGER.info("Returning {} rows from scanner {} for table: {}", cellSetModel.getRows().size(), scannerId, tableName);

        final byte[] protobufOutput = cellSetModel.createProtobufOutput();

        return Buffer.buffer(protobufOutput);
    }

    /**
     * Build an HBase RowModel for the given {@code result}.
     *
     * @param result
     * @param rowKey
     * @return a row
     */
    private RowModel buildRow(JsonObject result, String rowKey) {
        final RowModel row = new RowModel();

        row.setKey(Bytes.toBytes(rowKey));

        // add cells in sorted order
        buildSortedCells(result).forEach(c -> {
            final CellModel cell = new CellModel();
            cell.setColumn(Bytes.toBytes(c.getFieldName()));
            cell.setValue(Bytes.toBytes(c.getFieldValue()));
            row.addCell(cell);
        });
        return row;
    }
}
