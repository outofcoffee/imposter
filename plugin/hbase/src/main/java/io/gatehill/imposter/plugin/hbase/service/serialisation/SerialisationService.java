package io.gatehill.imposter.plugin.hbase.service.serialisation;

import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import io.gatehill.imposter.plugin.hbase.model.ResultCell;
import io.gatehill.imposter.plugin.hbase.model.ResultCellComparator;
import io.gatehill.imposter.plugin.hbase.service.ScannerService;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface SerialisationService {
    Logger getLogger();

    ScannerService getScannerService();

    Buffer serialise(String tableName, String recordId, JsonObject result);

    Buffer serialise(String tableName, String scannerId, JsonArray results, InMemoryScanner scanner, int rows);

    default String buildRowKey(InMemoryScanner scanner) {
        // TODO consider setting key to prefix from scanner filter
        return "rowKey" + scanner.getRowCounter().incrementAndGet();
    }

    default List<ResultCell> buildSortedCells(JsonObject result) {
        // add cells from result
        final List<ResultCell> cells = result.fieldNames().stream()
                .map(fieldName -> new ResultCell(fieldName, result.getString(fieldName)))
                .collect(Collectors.toList());

        // sort the cells before adding to row
        cells.sort(new ResultCellComparator());
        return cells;
    }

    default void checkExhausted(String tableName, String scannerId, JsonArray results, InMemoryScanner scanner) {
        final boolean exhausted = (scanner.getRowCounter().get() >= results.size());
        if (exhausted) {
            getLogger().info("Scanner {} for table: {} exhausted", scannerId, tableName);
            getScannerService().invalidateScanner(Integer.valueOf(scannerId));
        }
    }
}
