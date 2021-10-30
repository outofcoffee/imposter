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

package io.gatehill.imposter.plugin.hbase;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.RequireModules;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.hbase.config.HBasePluginConfig;
import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import io.gatehill.imposter.plugin.hbase.model.MockScanner;
import io.gatehill.imposter.plugin.hbase.model.RecordInfo;
import io.gatehill.imposter.plugin.hbase.model.ResponsePhase;
import io.gatehill.imposter.plugin.hbase.service.ScannerService;
import io.gatehill.imposter.plugin.hbase.service.serialisation.DeserialisationService;
import io.gatehill.imposter.plugin.hbase.service.serialisation.SerialisationService;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.util.FileUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.gatehill.imposter.plugin.ScriptedPlugin.scriptHandler;
import static io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Plugin for HBase.
 *
 * @author Pete Cornish
 */
@PluginInfo("hbase")
@RequireModules(HBasePluginModule.class)
public class HBasePluginImpl extends ConfiguredPlugin<HBasePluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(HBasePluginImpl.class);

    private final ResourceService resourceService;
    private final ResponseService responseService;
    private final ScannerService scannerService;

    private Map<String, HBasePluginConfig> tableConfigs;

    @Inject
    public HBasePluginImpl(Vertx vertx, ImposterConfig imposterConfig, ResourceService resourceService, ResponseService responseService, ScannerService scannerService) {
        super(vertx, imposterConfig);
        this.resourceService = resourceService;
        this.responseService = responseService;
        this.scannerService = scannerService;
    }

    @Override
    protected Class<HBasePluginConfig> getConfigClass() {
        return HBasePluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<? extends HBasePluginConfig> configs) {
        this.tableConfigs = configs.stream()
                .collect(Collectors.toConcurrentMap(HBasePluginConfig::getTableName, hBaseConfig -> hBaseConfig));
    }

    @Override
    public void configureRoutes(Router router) {
        // add route for each distinct path
        tableConfigs.values().stream()
                .map(config -> new ConfigAndPath(config, ofNullable(config.getPath()).orElse("")))
                .distinct()
                .forEach(configAndPath -> {
                    LOGGER.debug("Adding routes for base path: {}", () -> (configAndPath.path.isEmpty() ? "<empty>" : configAndPath.path));

                    // endpoint to allow individual row retrieval
                    addRowRetrievalRoute(configAndPath.config, router, configAndPath.path);

                    // Note: when scanning for results, the first call obtains a scanner:
                    addCreateScannerRoute(configAndPath.config, router, configAndPath.path);
                    // ...and the second call returns the results
                    addReadScannerResultsRoute(configAndPath.config, router, configAndPath.path);
                });
    }

    /**
     * Handles a request for a particular row within a table.
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private void addRowRetrievalRoute(PluginConfig pluginConfig, Router router, String path) {
        router.get(path + "/:tableName/:recordId/").handler(resourceService.handleRoute(getImposterConfig(), pluginConfig, getVertx(), routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");
            final String recordId = routingContext.request().getParam("recordId");

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received row request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received request for row with ID: {} for table: {}", recordId, tableName);
            final HBasePluginConfig config = tableConfigs.get(tableName);
            final RecordInfo recordInfo = new RecordInfo(recordId);

            // script should fire first
            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.RECORD, tableName, recordInfo, empty());
            scriptHandler(config, routingContext, getInjector(), bindings, responseBehaviour -> {
                // find the right row from results
                final JsonArray results = responseService.loadResponseAsJsonArray(config, responseBehaviour);
                final Optional<JsonObject> result = FileUtil.findRow(config.getIdField(), recordInfo.getRecordId(), results);

                final HttpServerResponse response = routingContext.response();
                if (result.isPresent()) {
                    final SerialisationService serialiser = findSerialiser(routingContext);
                    final Buffer buffer = serialiser.serialise(tableName, recordInfo.getRecordId(), result.get());
                    response.setStatusCode(HttpUtil.HTTP_OK)
                            .end(buffer);
                } else {
                    // no such record
                    LOGGER.error("No row found with ID: {} for table: {}", recordInfo.getRecordId(), tableName);
                    response.setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                            .end();
                }
            });
        }));
    }

    /**
     * Handles the first part of a request for results - creation of a scanner. Results are read from the scanner
     * in the handler {@link #addReadScannerResultsRoute(HBasePluginConfig, Router, String)}.
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private void addCreateScannerRoute(PluginConfig pluginConfig, Router router, String path) {
        router.post(path + "/:tableName/scanner").handler(resourceService.handleRoute(getImposterConfig(), pluginConfig, getVertx(), routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received scanner request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received scanner request for table: {}", tableName);
            final HBasePluginConfig config = tableConfigs.get(tableName);

            final DeserialisationService deserialiser = findDeserialiser(routingContext);

            final MockScanner scanner;
            try {
                scanner = deserialiser.decodeScanner(routingContext);
            } catch (Exception e) {
                routingContext.fail(e);
                return;
            }

            final Optional<String> scannerFilterPrefix = deserialiser.decodeScannerFilterPrefix(scanner);
            LOGGER.debug("Scanner filter for table: {}: {}, with prefix: {}", tableName, scanner.getFilter(), scannerFilterPrefix);

            // assert prefix matches if present
            if (ofNullable(config.getPrefix())
                    .map(configPrefix -> scannerFilterPrefix.map(configPrefix::equals).orElse(false))
                    .orElse(true)) {

                LOGGER.info("Scanner filter prefix matches expected value: {}", config.getPrefix());

            } else {
                LOGGER.error("Scanner filter prefix '{}' does not match expected value: {}}",
                        scannerFilterPrefix, config.getPrefix());

                routingContext.fail(HttpUtil.HTTP_INTERNAL_ERROR);
                return;
            }

            // script should fire first
            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.SCANNER, tableName, null, scannerFilterPrefix);
            scriptHandler(config, routingContext, getInjector(), bindings, responseBehaviour -> {
                final int scannerId = scannerService.registerScanner(config, scanner);

                final String resultUrl = getImposterConfig().getServerUrl() + path + "/" + tableName + "/scanner/" + scannerId;

                routingContext.response()
                        .putHeader("Location", resultUrl)
                        .setStatusCode(HttpUtil.HTTP_CREATED)
                        .end();
            });
        }));
    }

    /**
     * Handles the second part of a request for results - reading rows from the scanner created
     * in {@link #addCreateScannerRoute(PluginConfig, Router, String)}.
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private void addReadScannerResultsRoute(HBasePluginConfig pluginConfig, Router router, String path) {
        router.get(path + "/:tableName/scanner/:scannerId").handler(resourceService.handleRoute(getImposterConfig(), pluginConfig, getVertx(), routingContext -> {
            final String tableName = routingContext.request().getParam("tableName");
            final String scannerId = routingContext.request().getParam("scannerId");

            // query param e.g. ?n=1
            final int rows = Integer.valueOf(routingContext.request().getParam("n"));

            // check that the table is registered
            if (!tableConfigs.containsKey(tableName)) {
                LOGGER.error("Received result request for unknown table: {}", tableName);

                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            // check that the scanner was created
            final Optional<InMemoryScanner> optionalScanner = scannerService.fetchScanner(Integer.valueOf(scannerId));
            if (!optionalScanner.isPresent()) {
                LOGGER.error("Received result request for non-existent scanner {} for table: {}", scannerId, tableName);

                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
                return;
            }

            LOGGER.info("Received result request for {} rows from scanner {} for table: {}", rows, scannerId, tableName);
            final InMemoryScanner scanner = optionalScanner.get();

            // load result
            final HBasePluginConfig config = tableConfigs.get(tableName);

            // script should fire first
            final DeserialisationService deserialiser = findDeserialiser(routingContext);

            final Map<String, Object> bindings = buildScriptBindings(ResponsePhase.RESULTS, tableName, null,
                    deserialiser.decodeScannerFilterPrefix(scanner.getScanner()));

            scriptHandler(config, routingContext, getInjector(), bindings, responseBehaviour -> {
                // build results
                final JsonArray results = responseService.loadResponseAsJsonArray(config, responseBehaviour);
                final SerialisationService serialiser = findSerialiser(routingContext);
                final Buffer buffer = serialiser.serialise(tableName, scannerId, results, scanner, rows);
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_OK)
                        .end(buffer);
            });
        }));
    }

    /**
     * Find the serialiser binding based on the content types accepted by the client.
     *
     * @param routingContext the Vert.x routing context
     * @return the serialiser
     */
    private SerialisationService findSerialiser(RoutingContext routingContext) {
        final List<String> acceptedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext);

        // add as default to end of the list
        if (!acceptedContentTypes.contains(CONTENT_TYPE_JSON)) {
            acceptedContentTypes.add(CONTENT_TYPE_JSON);
        }

        // search the ordered list
        for (String contentType : acceptedContentTypes) {
            try {
                final SerialisationService serialiser = getInjector().getInstance(Key.get(SerialisationService.class, Names.named(contentType)));
                LOGGER.debug("Found serialiser binding {} for content type '{}'", serialiser.getClass().getSimpleName(), contentType);
                return serialiser;

            } catch (Exception e) {
                LOGGER.trace("Unable to load serialiser binding for content type '{}'", contentType, e);
            }
        }

        throw new RuntimeException(String.format(
                "Unable to find serialiser matching any accepted content type: %s", acceptedContentTypes));
    }

    /**
     * Find the deserialiser binding based on the content types sent by the client.
     *
     * @param routingContext the Vert.x routing context
     * @return the deserialiser
     */
    private DeserialisationService findDeserialiser(RoutingContext routingContext) {
        String contentType = routingContext.request().getHeader("Content-Type");

        // use JSON as default
        if (Strings.isNullOrEmpty(contentType)) {
            contentType = CONTENT_TYPE_JSON;
        }

        try {
            final DeserialisationService deserialiser = getInjector().getInstance(Key.get(DeserialisationService.class, Names.named(contentType)));
            LOGGER.debug("Found deserialiser binding {} for content type '{}'", deserialiser.getClass().getSimpleName(), contentType);
            return deserialiser;

        } catch (Exception e) {
            LOGGER.trace("Unable to load deserialiser binding for content type '{}'", contentType, e);
        }

        throw new RuntimeException(String.format(
                "Unable to find deserialiser matching content type: %s", contentType));
    }

    /**
     * Add additional script bindings.
     *
     * @param responsePhase
     * @param tableName
     * @param scannerFilterPrefix
     * @return
     */
    private Map<String, Object> buildScriptBindings(ResponsePhase responsePhase, String tableName, RecordInfo recordInfo, Optional<String> scannerFilterPrefix) {
        final Map<String, Object> bindings = Maps.newHashMap();
        bindings.put("tableName", tableName);
        bindings.put("recordInfo", recordInfo);
        bindings.put("responsePhase", responsePhase);
        bindings.put("scannerFilterPrefix", scannerFilterPrefix.orElse(""));
        return bindings;
    }

    private static class ConfigAndPath {
        final HBasePluginConfig config;
        final String path;

        public ConfigAndPath(HBasePluginConfig config, String path) {
            this.config = config;
            this.path = path;
        }

        /**
         * Only path is used for equality/hash code.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigAndPath that = (ConfigAndPath) o;
            return Objects.equal(path, that.path);
        }

        /**
         * Only path is used for equality/hash code.
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(path);
        }
    }
}
