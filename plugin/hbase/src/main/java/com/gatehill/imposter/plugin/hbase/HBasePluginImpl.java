package com.gatehill.imposter.plugin.hbase;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.ScriptedPlugin;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import com.gatehill.imposter.plugin.hbase.model.ResponsePhase;
import com.gatehill.imposter.plugin.hbase.model.ResultCell;
import com.gatehill.imposter.plugin.hbase.model.ResultCellComparator;
import com.gatehill.imposter.service.ResponseService;
import com.gatehill.imposter.util.FileUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HBasePluginImpl extends ConfiguredPlugin<HBasePluginConfig> implements ScriptedPlugin<HBasePluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(HBasePluginImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    @javax.inject.Inject
    private ResponseService responseService;

    private Map<String, HBasePluginConfig> tableConfigs;
    private AtomicInteger scannerIdCounter = new AtomicInteger();

    /**
     * Hold scanners for a period of time.
     */
    private Cache<Integer, InMemoryScanner> createdScanners = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    protected Class<HBasePluginConfig> getConfigClass() {
        return HBasePluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<HBasePluginConfig> configs) {
        this.tableConfigs = configs.stream()
                .collect(Collectors.toConcurrentMap(HBasePluginConfig::getTableName, hBaseConfig -> hBaseConfig));
    }

    @Override
    public void configureRoutes(Router router) {
        // add route for each distinct path
        tableConfigs.values().stream()
                .map(config -> ofNullable(config.getPath()).orElse(""))
                .distinct()
                .forEach(path -> {
                    LOGGER.debug("Adding routes for base path: {}", () -> (path.isEmpty() ? "<empty>" : path));

                    // endpoint to allow individual row retrieval
                    addRowRetrievalRoute(router, path);

                    // Note: when scanning for results, the first call obtains a scanner:
                    addCreateScannerRoute(router, path);
                    // ...and the second call returns the results
                    addReadScannerResultsRoute(router, path);
                });
    }

    /**
     * Handles a request for a particular row within a table.
     *
     * @param router
     * @param path
     */
    private void addRowRetrievalRoute(Router router, String path) {
        router.get(path + "/:tableName/:recordId/").handler(routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");
            final String recordId = routingContext.request().getParam("recordId");

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received row request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received request for row with ID: {} for table: {}", recordId, tableName);
            final HBasePluginConfig config = tableConfigs.get(tableName);

            // script should fire first
            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.RECORD, empty());
            scriptHandler(config, routingContext, bindings, responseBehaviour -> {

                // find the right row from results
                final JsonArray results = responseService.loadResponseAsJsonArray(responseBehaviour);
                final Optional<JsonObject> result = FileUtil.findRow(config.getIdField(), recordId, results);

                final HttpServerResponse response = routingContext.response();
                if (result.isPresent()) {
                    final CellSetModel cellSetModel = new CellSetModel();
                    cellSetModel.addRow(buildRow(result.get(), recordId));

                    LOGGER.info("Returning single row with ID: {} for table: {}", recordId, tableName);

                    final byte[] protobufOutput = cellSetModel.createProtobufOutput();

                    response.setStatusCode(HttpURLConnection.HTTP_OK)
                            .end(Buffer.buffer(protobufOutput));
                } else {
                    // no such record
                    LOGGER.error("No row found with ID: {} for table: {}", recordId, tableName);
                    response.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                            .end();
                }
            });
        });
    }

    /**
     * Handles the first part of a request for results - creation of a scanner. Results are read from the scanner
     * in the handler {@link #addReadScannerResultsRoute(Router, String)}.
     *
     * @param router
     * @param path
     */
    private void addCreateScannerRoute(Router router, String path) {
        router.post(path + "/:tableName/scanner").handler(routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received scanner request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received scanner request for table: {}", tableName);
            final HBasePluginConfig config = tableConfigs.get(tableName);

            // parse request
            final ScannerModel scannerModel;
            try {
                scannerModel = getScannerModel(routingContext);
            } catch (IOException e) {
                routingContext.fail(e);
                return;
            }

            final Optional<String> scannerFilterPrefix = getScannerFilterPrefix(scannerModel);
            LOGGER.debug("Scanner filter for table: {}: {}, with prefix: {}", tableName, scannerModel.getFilter(), scannerFilterPrefix);

            // assert prefix matches if present
            if (ofNullable(config.getPrefix())
                    .map(configPrefix -> scannerFilterPrefix.map(configPrefix::equals).orElse(false))
                    .orElse(true)) {

                LOGGER.info("Scanner filter prefix matches expected value: {}", config.getPrefix());

            } else {
                LOGGER.error("Scanner filter prefix '{}' does not match expected value: {}}",
                        scannerFilterPrefix, config.getPrefix());

                routingContext.fail(HttpURLConnection.HTTP_INTERNAL_ERROR);
                return;
            }

            // script should fire first
            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.SCANNER, scannerFilterPrefix);
            scriptHandler(config, routingContext, bindings, responseBehaviour -> {

                // register scanner
                final int scannerId = scannerIdCounter.incrementAndGet();
                createdScanners.put(scannerId, new InMemoryScanner(config, scannerModel));

                final String resultUrl = imposterConfig.getServerUrl() + path + "/" + tableName + "/scanner/" + scannerId;

                routingContext.response()
                        .putHeader("Location", resultUrl)
                        .setStatusCode(HttpURLConnection.HTTP_CREATED)
                        .end();
            });
        });
    }

    /**
     * Handles the second part of a request for results - reading rows from the scanner created
     * in {@link #addCreateScannerRoute(Router, String)}.
     *
     * @param router
     * @param path
     */
    private void addReadScannerResultsRoute(Router router, String path) {
        router.get(path + "/:tableName/scanner/:scannerId").handler(routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");
            final String scannerId = routingContext.request().getParam("scannerId");

            // query param e.g. ?n=1
            final int rows = Integer.valueOf(routingContext.request().getParam("n"));

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received result request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            // check that the scanner was created
            final Optional<InMemoryScanner> optionalScanner = ofNullable(createdScanners.getIfPresent(Integer.valueOf(scannerId)));
            if (!optionalScanner.isPresent()) {
                LOGGER.error("Received result request for non-existent scanner {} for table: {}", scannerId, tableName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received result request for {} rows from scanner {} for table: {}", rows, scannerId, tableName);
            final InMemoryScanner scanner = optionalScanner.get();

            // load result
            final HBasePluginConfig config = tableConfigs.get(tableName);

            // script should fire first
            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.RESULTS, getScannerFilterPrefix(scanner.getScanner()));
            scriptHandler(config, routingContext, bindings, responseBehaviour -> {

                // build results
                final JsonArray results = responseService.loadResponseAsJsonArray(responseBehaviour);
                final CellSetModel cellSetModel = new CellSetModel();

                // start the row counter from the last position in the scanner
                final int lastPosition = scanner.getRowCounter().get();
                for (int i = lastPosition; i < (lastPosition + rows) && i < results.size(); i++) {
                    final JsonObject result = results.getJsonObject(i);

                    final RowModel row = buildRow(result, "rowKey" + scanner.getRowCounter().incrementAndGet());

                    cellSetModel.addRow(row);
                }

                final boolean exhausted = (scanner.getRowCounter().get() >= results.size());
                if (exhausted) {
                    LOGGER.info("Scanner {} for table: {} exhausted", scannerId, tableName);
                    createdScanners.invalidate(scannerId);
                }

                LOGGER.info("Returning {} rows from scanner {} for table: {}", cellSetModel.getRows().size(), scannerId, tableName);

                final byte[] protobufOutput = cellSetModel.createProtobufOutput();

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_OK)
                        .end(Buffer.buffer(protobufOutput));
            });

        });
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

        // TODO consider setting key to prefix from scanner filter
        row.setKey(Bytes.toBytes(rowKey));

        // add cells from result file
        final List<ResultCell> cells = result.fieldNames().stream()
                .map(fieldName -> new ResultCell(fieldName, result.getString(fieldName)))
                .collect(Collectors.toList());

        // sort the cells before adding to row
        cells.sort(new ResultCellComparator());

        // add cells in sorted order
        cells.forEach(c -> {
            final CellModel cell = new CellModel();
            cell.setColumn(Bytes.toBytes(c.getFieldName()));
            cell.setValue(Bytes.toBytes(c.getFieldValue()));
            row.addCell(cell);
        });
        return row;
    }

    /**
     * Add additional script bindings.
     *
     * @param responsePhase
     * @param scannerFilterPrefix
     * @return
     */
    private Map<String, Object> buildScriptBindings(ResponsePhase responsePhase, Optional<String> scannerFilterPrefix) {
        final Map<String, Object> bindings = Maps.newHashMap();
        bindings.put("responsePhase", responsePhase);
        bindings.put("scannerFilterPrefix", scannerFilterPrefix.orElse(""));
        return bindings;
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

    /**
     * @param scannerModel
     * @return the scanner filter prefix
     */
    private Optional<String> getScannerFilterPrefix(ScannerModel scannerModel) {
        final Filter filter;
        try {
            filter = ScannerModel.buildFilter(scannerModel.getFilter());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (filter instanceof PrefixFilter) {
            final String prefix = Bytes.toString(((PrefixFilter) filter).getPrefix());
            return Optional.of(prefix);
        }
        return empty();
    }
}
