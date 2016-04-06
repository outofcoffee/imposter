package com.gatehill.imposter;

import com.gatehill.imposter.plugin.Plugin;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.apache.hadoop.hbase.rest.model.CellModel;
import org.apache.hadoop.hbase.rest.model.CellSetModel;
import org.apache.hadoop.hbase.rest.model.RowModel;
import org.apache.hadoop.hbase.rest.model.ScannerModel;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pcornish
 */
public class HBasePluginImpl implements Plugin {
    private static final Logger LOGGER = LogManager.getLogger(HBasePluginImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    private AtomicInteger scannerIdCounter = new AtomicInteger();
    private Map<Integer, MockScanner> createdScanners = Maps.newConcurrentMap();
    private Map<String, BaseMockConfig> mockConfigs;

    @Override
    public void configureRoutes(Router router) {
        // the first call obtains a scanner
        addCreateScannerRoute(router);

        // the second call returns the result
        addReadScannerResultsRoute(router);
    }

    @Override
    public void configureMocks(Map<String, BaseMockConfig> mockConfigs) {
        this.mockConfigs = mockConfigs;
    }

    private void addCreateScannerRoute(Router router) {
        router.post("/gateway/default/hbase/:viewName/scanner").handler(routingContext -> {
            final String viewName = routingContext.request().getParam("viewName");

            // check that the view is registered
            if (!mockConfigs.containsKey(viewName)) {
                LOGGER.error("Received scanner request for unregistered view: {}", viewName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received scanner request for registered view: {}", viewName);

            // parse request
            final ScannerModel scannerModel;
            try {
                final byte[] rawMessage = routingContext.getBody().getBytes();
                scannerModel = new ScannerModel();
                scannerModel.getObjectFromMessage(rawMessage);

            } catch (IOException e) {
                routingContext.fail(e);
                return;
            }

            LOGGER.debug("{} scanner filter: {}", viewName, scannerModel.getFilter());

            // register scanner
            final int scannerId = scannerIdCounter.incrementAndGet();
            createdScanners.put(scannerId, new MockScanner(mockConfigs.get(viewName), scannerModel));

            final String resultPath = "/gateway/default/hbase/" + viewName + "/scanner/" + scannerId;
            final String resultUrl = String.format("http://%s:%s", imposterConfig.getHost(), imposterConfig.getListenPort()) + resultPath;

            routingContext.response()
                    .putHeader("Location", resultUrl)
                    .setStatusCode(HttpURLConnection.HTTP_CREATED)
                    .end();
        });
    }

    private void addReadScannerResultsRoute(Router router) {
        router.get("/gateway/default/hbase/:viewName/scanner/:scannerId").handler(routingContext -> {
            final String viewName = routingContext.request().getParam("viewName");
            final String scannerId = routingContext.request().getParam("scannerId");

            // query param e.g. ?n=1
            final int rows = Integer.valueOf(routingContext.request().getParam("n"));

            // check that the view is registered
            if (!mockConfigs.containsKey(viewName)) {
                LOGGER.error("Received mock result request for unregistered view: {}", viewName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            // check that the scanner was created
            final BaseMockConfig mockConfig = mockConfigs.get(viewName);
            if (!createdScanners.containsKey(Integer.valueOf(scannerId))) {
                LOGGER.error("Received mock result request for non-existent scanner {} for view: {}", scannerId, viewName);

                routingContext.response()
                        .setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received mock result request for {} rows from scanner {} for registered view: {}", rows, scannerId, viewName);
            final MockScanner scanner = createdScanners.get(Integer.valueOf(scannerId));

            // load result
            final JsonArray results;
            try (InputStream is = Files.newInputStream(Paths.get(imposterConfig.getConfigDir(), mockConfig.getResponseFile()))) {
                results = new JsonArray(CharStreams.toString(new InputStreamReader(is)));

            } catch (IOException e) {
                routingContext.fail(e);
                return;
            }

            // build results
            int rowCounter = 0;
            final CellSetModel cellSetModel = new CellSetModel();

            for (int i = 0; i < rows && i < results.size(); i++) {
                final JsonObject result = results.getJsonObject(i);

                final RowModel row = new RowModel();

                // TODO set to prefix from scanner filter
                row.setKey(Bytes.toBytes("rowKey" + ++rowCounter));

                // add cells from result file
                for (String fieldName : result.fieldNames()) {
                    final CellModel cell = new CellModel();
                    cell.setColumn(Bytes.toBytes(fieldName));
                    cell.setValue(Bytes.toBytes(result.getString(fieldName)));
                    row.addCell(cell);
                }

                cellSetModel.addRow(row);
            }

            final byte[] protobufOutput = cellSetModel.createProtobufOutput();

            routingContext.response()
                    .setStatusCode(HttpURLConnection.HTTP_OK)
                    .end(Buffer.buffer(protobufOutput));
        });
    }
}
