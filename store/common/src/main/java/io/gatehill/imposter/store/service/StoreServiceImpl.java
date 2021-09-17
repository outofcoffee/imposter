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

package io.gatehill.imposter.store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.lifecycle.ImposterLifecycleListener;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.capture.CaptureConfig;
import io.gatehill.imposter.plugin.config.capture.CaptureConfigHolder;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.system.StoreConfig;
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.model.StoreFactory;
import io.gatehill.imposter.store.model.StoreHolder;
import io.gatehill.imposter.store.util.StoreUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.MapUtil;
import io.gatehill.imposter.util.ResourceUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreServiceImpl implements StoreService, ImposterLifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger(StoreServiceImpl.class);
    private static final ParsableMIMEValue JSON_MIME = new ParsableMIMEValue(HttpUtil.CONTENT_TYPE_JSON);

    /**
     * Default to request scope unless specified.
     */
    private static final String DEFAULT_CAPTURE_STORE_NAME = StoreUtil.REQUEST_SCOPED_STORE_NAME;

    private static final ParseContext JSONPATH_PARSE_CONTEXT = JsonPath.using(Configuration.builder()
            .mappingProvider(new JacksonMappingProvider())
            .build());

    private final Vertx vertx;
    private final ResourceService resourceService;
    private final StoreFactory storeFactory;
    private final StringSubstitutor storeItemSubstituter;

    @Inject
    public StoreServiceImpl(
            Vertx vertx,
            ImposterLifecycleHooks lifecycleHooks,
            ResourceService resourceService,
            StoreFactory storeFactory
    ) {
        this.vertx = vertx;
        this.resourceService = resourceService;
        this.storeFactory = storeFactory;

        LOGGER.debug("Stores enabled");
        storeItemSubstituter = buildStoreItemSubstituter();
        lifecycleHooks.registerListener(this);
    }

    /**
     * @return a string substituter that replaces placeholders like 'example.foo' with the value of the
     * item 'foo' in the store 'example'.
     */
    private StringSubstitutor buildStoreItemSubstituter() {
        final StringLookup variableResolver = StringLookupFactory.INSTANCE.functionStringLookup(key -> {
            try {
                final int dotIndex = key.indexOf(".");
                if (dotIndex > 0) {
                    final String storeName = key.substring(0, dotIndex);
                    if (storeFactory.hasStoreWithName(storeName)) {
                        final String itemKey = key.substring(dotIndex + 1);
                        return loadItemFromStore(storeName, itemKey);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error replacing template placeholder '%s' with store item", key), e);
            }
            throw new IllegalStateException("Unknown store for template placeholder: " + key);
        });

        return new StringSubstitutor(variableResolver);
    }

    private Object loadItemFromStore(String storeName, String itemKey) {
        // check for jsonpath expression
        final int colonIndex = itemKey.indexOf(":");
        final String jsonPath;
        if (colonIndex > 0) {
            jsonPath = itemKey.substring(colonIndex + 1);
            itemKey = itemKey.substring(0, colonIndex);
        } else {
            jsonPath = null;
        }

        final Store store = storeFactory.getStoreByName(storeName, false);
        final Object itemValue = store.load(itemKey);

        if (nonNull(jsonPath)) {
            return JSONPATH_PARSE_CONTEXT.parse(itemValue).read(jsonPath);
        } else {
            return itemValue;
        }
    }

    @Override
    public void afterRoutesConfigured(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs, Router router) {
        router.get("/system/store/:storeName").handler(handleLoadAll(imposterConfig, allPluginConfigs));
        router.delete("/system/store/:storeName").handler(handleDeleteStore(imposterConfig, allPluginConfigs));
        router.get("/system/store/:storeName/:key").handler(handleLoadSingle(imposterConfig, allPluginConfigs));
        router.put("/system/store/:storeName/:key").handler(handleSaveSingle(imposterConfig, allPluginConfigs));
        router.post("/system/store/:storeName").handler(handleSaveMultiple(imposterConfig, allPluginConfigs));
        router.delete("/system/store/:storeName/:key").handler(handleDeleteSingle(imposterConfig, allPluginConfigs));

        preloadStores(allPluginConfigs);
    }

    private void preloadStores(List<PluginConfig> allPluginConfigs) {
        allPluginConfigs.forEach(pluginConfig -> {
            if (pluginConfig instanceof SystemConfigHolder) {
                ofNullable(((SystemConfigHolder) pluginConfig).getSystemConfig())
                        .flatMap(systemConfig -> ofNullable(systemConfig.getStoreConfigs()))
                        .ifPresent(storeConfigs -> storeConfigs.forEach((storeName, storeConfig) -> preload(storeName, pluginConfig, storeConfig)));
            }
        });
    }

    private void preload(String storeName, PluginConfig pluginConfig, StoreConfig storeConfig) {
        // validate config
        if (StoreUtil.isRequestScopedStore(storeName)) {
            throw new IllegalStateException("Cannot preload request scoped store: " + storeName);
        }

        final Store store = storeFactory.getStoreByName(storeName, false);
        final Map<String, Object> preloadData = storeConfig.getPreloadData();
        if (nonNull(preloadData)) {
            LOGGER.trace("Preloading inline data into store: {}", storeName);
            preloadData.forEach(store::save);
            LOGGER.debug("Preloaded {} items from inline data into store: {}", preloadData.size(), storeName);

        } else if (nonNull(storeConfig.getPreloadFile())) {
            if (!storeConfig.getPreloadFile().endsWith(".json")) {
                throw new IllegalStateException("Only JSON (.json) files containing a top-level object are supported for preloading");
            }

            final Path preloadPath = Paths.get(pluginConfig.getParentDir().getPath(), storeConfig.getPreloadFile()).toAbsolutePath();
            LOGGER.trace("Preloading file {} into store: {}", preloadPath, storeName);

            try {
                @SuppressWarnings("unchecked")
                final Map<String, Object> fileContents = MapUtil.JSON_MAPPER.readValue(preloadPath.toFile(), Map.class);
                fileContents.forEach(store::save);
                LOGGER.debug("Preloaded {} items from file {} into store: {}", fileContents.size(), preloadPath, storeName);

            } catch (IOException e) {
                throw new RuntimeException(String.format("Error preloading file %s into store: %s", preloadPath, storeName), e);
            }
        }
    }

    private Handler<RoutingContext> handleLoadAll(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName);
            if (isNull(store)) {
                return;
            }

            final List<MIMEHeader> accepted = routingContext.parsedHeaders().accept();
            if (accepted.isEmpty() || accepted.stream().anyMatch(a -> a.isMatchedBy(JSON_MIME))) {
                LOGGER.debug("Listing store: {}", storeName);
                serialiseBodyAsJson(routingContext, store.loadAll());

            } else {
                // client doesn't accept JSON
                LOGGER.warn("Cannot serialise store: {} as client does not accept JSON", storeName);
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_ACCEPTABLE)
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end("Stores are only available as JSON. Please set an appropriate Accept header.");
            }
        });
    }

    private Handler<RoutingContext> handleDeleteStore(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");

            storeFactory.deleteStoreByName(storeName);
            LOGGER.debug("Deleted store: {}", storeName);

            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                    .end();
        });
    }

    private Handler<RoutingContext> handleLoadSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");
            final Object value = store.load(key);
            if (nonNull(value)) {
                if (value instanceof String) {
                    LOGGER.debug("Returning string item: {} from store: {}", key, storeName);
                    routingContext.response()
                            .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                            .end((String) value);
                } else {
                    LOGGER.debug("Returning object item: {} from store: {}", key, storeName);
                    serialiseBodyAsJson(routingContext, value);
                }
            } else {
                LOGGER.debug("Nonexistent item: {} in store: {}", key, storeName);
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
            }
        });
    }

    private Handler<RoutingContext> handleSaveSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");

            // "If the target resource does not have a current representation and the
            // PUT successfully creates one, then the origin server MUST inform the
            // user agent by sending a 201 (Created) response."
            // See: https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.4
            final int statusCode = store.hasItemWithKey(key) ? HttpUtil.HTTP_OK : HttpUtil.HTTP_CREATED;

            final String value = routingContext.getBodyAsString();
            store.save(key, value);

            LOGGER.debug("Saved item: {} to store: {}", key, storeName);
            routingContext.response()
                    .setStatusCode(statusCode)
                    .end();
        });
    }

    private Handler<RoutingContext> handleSaveMultiple(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final Map<String, Object> items = routingContext.getBodyAsJson().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            items.forEach(store::save);
            LOGGER.debug("Saved {} items to store: {}", items.size(), storeName);

            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_OK)
                    .end();
        });
    }

    private Handler<RoutingContext> handleDeleteSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");
            store.delete(key);

            LOGGER.debug("Deleted item: {} from store: {}", key, storeName);
            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                    .end();
        });
    }

    private Store openStore(RoutingContext routingContext, String storeName) {
        return openStore(routingContext, storeName, false);
    }

    private Store openStore(RoutingContext routingContext, String storeName, boolean createIfNotExist) {
        if (!storeFactory.hasStoreWithName(storeName)) {
            LOGGER.debug("No store found named: {}", storeName);

            if (!createIfNotExist) {
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end(String.format("No store named '%s'.", storeName));
                return null;
            }
            // ...otherwise fall through and implicitly create below
        }

        return storeFactory.getStoreByName(storeName, false);
    }

    private void serialiseBodyAsJson(RoutingContext routingContext, Object body) {
        try {
            routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(MapUtil.JSON_MAPPER.writeValueAsString(body));

        } catch (JsonProcessingException e) {
            routingContext.fail(e);
        }
    }

    @Override
    public void beforeBuildingResponse(RoutingContext routingContext, ResponseConfigHolder resourceConfig) {
        if (resourceConfig instanceof CaptureConfigHolder) {
            final Map<String, CaptureConfig> captureConfig = ((CaptureConfigHolder) resourceConfig).getCaptureConfig();
            if (nonNull(captureConfig)) {
                final AtomicReference<DocumentContext> jsonPathContextHolder = new AtomicReference<>();

                captureConfig.forEach((itemName, itemConfig) -> {
                    final Object itemValue;
                    try {
                        itemValue = captureValue(routingContext, itemConfig, jsonPathContextHolder);
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Error capturing item: %s", itemName), e);
                    }

                    final String storeName = ofNullable(itemConfig.getStore()).orElse(DEFAULT_CAPTURE_STORE_NAME);
                    LOGGER.debug("Capturing item: {} into store: {}", itemName, storeName);

                    final Store store = openCaptureStore(routingContext, storeName);
                    store.save(itemName, itemValue);
                });
            }
        }
    }

    private Store openCaptureStore(RoutingContext routingContext, String storeName) {
        final Store store;
        if (StoreUtil.isRequestScopedStore(storeName)) {
            final String uniqueRequestId = routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY);
            storeName = StoreUtil.buildRequestStoreName(uniqueRequestId);
            store = storeFactory.getStoreByName(storeName, true);
        } else {
            store = storeFactory.getStoreByName(storeName, false);
        }
        return store;
    }

    private Object captureValue(RoutingContext routingContext, CaptureConfig itemConfig, AtomicReference<DocumentContext> jsonPathContextHolder) {
        if (!Strings.isNullOrEmpty(itemConfig.getPathParam())) {
            return routingContext.pathParam(itemConfig.getPathParam());
        } else if (!Strings.isNullOrEmpty(itemConfig.getQueryParam())) {
            return routingContext.queryParam(itemConfig.getQueryParam()).stream().findFirst().orElse(null);
        } else if (!Strings.isNullOrEmpty(itemConfig.getRequestHeader())) {
            return routingContext.request().getHeader(itemConfig.getRequestHeader());
        } else if (!Strings.isNullOrEmpty(itemConfig.getJsonPath())) {
            DocumentContext jsonPathContext = jsonPathContextHolder.get();
            if (isNull(jsonPathContext)) {
                jsonPathContext = JSONPATH_PARSE_CONTEXT.parse(routingContext.getBodyAsString());
                jsonPathContextHolder.set(jsonPathContext);
            }
            return jsonPathContext.read(itemConfig.getJsonPath());
        } else {
            return null;
        }
    }

    @Override
    public void beforeBuildingRuntimeContext(RoutingContext routingContext, Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        final String requestId = routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY);
        additionalBindings.put("stores", new StoreHolder(storeFactory, requestId));
    }

    @Override
    public String beforeTransmittingTemplate(RoutingContext routingContext, String responseTemplate) {
        // shim for request scoped store
        final String uniqueRequestId = routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY);
        responseTemplate = responseTemplate.replaceAll("\\$\\{request\\.", "\\${request_" + uniqueRequestId + ".");

        return storeItemSubstituter.replace(responseTemplate);
    }

    @Override
    public void afterRoutingContextHandled(RoutingContext routingContext) {
        // clean up request store if one exists
        ofNullable((String) routingContext.get(ResourceUtil.RC_REQUEST_ID_KEY)).ifPresent(uniqueRequestId ->
                storeFactory.deleteStoreByName(StoreUtil.buildRequestStoreName(uniqueRequestId))
        );
    }
}
