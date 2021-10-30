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

package io.gatehill.imposter.plugin.sfdc;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.sfdc.config.SfdcPluginConfig;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.util.FileUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

import static io.gatehill.imposter.plugin.ScriptedPlugin.scriptHandler;
import static io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * Plugin for SFDC.
 *
 * @author Pete Cornish
 */
@PluginInfo("sfdc")
public class SfdcPluginImpl extends ConfiguredPlugin<SfdcPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(SfdcPluginImpl.class);
    private static final String FIELD_ID = "Id";

    private final ResourceService resourceService;
    private final ResponseService responseService;

    private List<? extends SfdcPluginConfig> configs;

    @Inject
    public SfdcPluginImpl(Vertx vertx, ImposterConfig imposterConfig, ResourceService resourceService, ResponseService responseService) {
        super(vertx, imposterConfig);
        this.resourceService = resourceService;
        this.responseService = responseService;
    }

    @Override
    protected Class<SfdcPluginConfig> getConfigClass() {
        return SfdcPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<? extends SfdcPluginConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        // oauth handler
        router.post("/services/oauth2/token").handler(resourceService.handleRoute(getImposterConfig(), configs, getVertx(), routingContext -> {
            LOGGER.info("Handling oauth request: {}", routingContext.getBodyAsString());

            final JsonObject authResponse = new JsonObject();
            authResponse.put("access_token", "dummyAccessToken");
            authResponse.put("instance_url", getImposterConfig().getServerUrl());
            routingContext.response().putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);
            routingContext.response().end(authResponse.encode());
        }));

        // query handler
        router.get("/services/data/:apiVersion/query/").handler(resourceService.handleRoute(getImposterConfig(), configs, getVertx(), routingContext -> {
            final String apiVersion = routingContext.request().getParam("apiVersion");

            // e.g. 'SELECT Name, Id from Account LIMIT 100'
            final String query = routingContext.request().getParam("q");

            final String sObjectName = getSObjectName(query)
                    .orElseThrow(() -> new RuntimeException(String.format("Could not determine SObject name from query: %s", query)));

            final SfdcPluginConfig config = configs.stream()
                    .filter(sfdcPluginConfig -> sObjectName.equalsIgnoreCase(sfdcPluginConfig.getsObjectName()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException(String.format("Unable to find mock config for SObject: %s", sObjectName)));

            // script should fire first
            scriptHandler(config, routingContext, getInjector(), responseBehaviour -> {

                // enrich records
                final JsonArray records = responseService.loadResponseAsJsonArray(config, responseBehaviour);
                for (int i = 0; i < records.size(); i++) {
                    addRecordAttributes(records.getJsonObject(i), apiVersion, config.getsObjectName());
                }

                final JsonObject responseWrapper = new JsonObject();
                responseWrapper.put("done", true);
                responseWrapper.put("records", records);
                responseWrapper.put("totalSize", records.size());

                LOGGER.info("Sending {} SObjects in response to query: {}", records.size(), query);

                final HttpServerResponse response = routingContext.response();

                response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .setStatusCode(HttpUtil.HTTP_OK)
                        .end(Buffer.buffer(responseWrapper.encodePrettily()));
            });
        }));

        // get SObject handler
        configs.forEach(config -> {
            router.get("/services/data/:apiVersion/sobjects/" + config.getsObjectName() + "/:sObjectId")
                    .handler(resourceService.handleRoute(getImposterConfig(), config, getVertx(), routingContext -> {
                        // script should fire first
                        scriptHandler(config, routingContext, getInjector(), responseBehaviour -> {

                            final String apiVersion = routingContext.request().getParam("apiVersion");
                            final String sObjectId = routingContext.request().getParam("sObjectId");

                            // find and enrich record
                            final Optional<JsonObject> result = FileUtil.findRow(FIELD_ID, sObjectId,
                                    responseService.loadResponseAsJsonArray(config, responseBehaviour))
                                    .map(r -> addRecordAttributes(r, apiVersion, config.getsObjectName()));

                            final HttpServerResponse response = routingContext.response();

                            if (result.isPresent()) {
                                LOGGER.info("Sending SObject with ID: {}", sObjectId);

                                response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                                        .setStatusCode(HttpUtil.HTTP_OK)
                                        .end(Buffer.buffer(result.get().encodePrettily()));
                            } else {
                                // no such record
                                LOGGER.error("{} SObject with ID: {} not found", config.getsObjectName(), sObjectId);
                                response.setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                                        .end();
                            }
                        });
                    }));
        });

        // create SObject handler
        router.post("/services/data/:apiVersion/sobjects/:sObjectName")
                .handler(resourceService.handleRoute(getImposterConfig(), configs, getVertx(), routingContext -> {
                    final String sObjectName = routingContext.request().getParam("sObjectName");
                    final JsonObject sObject = routingContext.getBodyAsJson();

                    LOGGER.info("Received create request for {}: {}", sObjectName, sObject);

                    final JsonObject result = new JsonObject();

                    // Note: ID response field name has to be lowercase, for some reason
                    result.put("id", generateBase62Id());
                    result.put("success", true);

                    routingContext.response()
                            .putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                            .setStatusCode(HttpUtil.HTTP_CREATED)
                            .end(Buffer.buffer(result.encodePrettily()));
                }));

        // update SObject handlers
        router.patch("/services/data/:apiVersion/sobjects/:sObjectName/:sObjectId")
                .handler(handleUpdateRequest());
        router.post("/services/data/:apiVersion/sobjects/:sObjectName/:sObjectId")
                .handler(handleUpdateRequest());
    }

    /**
     * Can be a PATCH or a POST request (with query parameter '_HttpMethod=PATCH').
     *
     * @return
     */
    private Handler<RoutingContext> handleUpdateRequest() {
        return resourceService.handleRoute(getImposterConfig(), configs, getVertx(), routingContext -> {
            final String sObjectName = routingContext.request().getParam("sObjectName");
            final String sObjectId = routingContext.request().getParam("sObjectId");
            final JsonObject sObject = routingContext.getBodyAsJson();

            // SFDC work-around for HTTP clients that don't support PATCH
            if (!HttpMethod.PATCH.equals(routingContext.request().method())
                    && !"PATCH".equals(routingContext.request().getParam("_HttpMethod"))) {

                routingContext.fail(HttpUtil.HTTP_BAD_METHOD);
                return;
            }

            LOGGER.info("Received update request for {} with ID: {}: {}", sObjectName, sObjectId, sObject);

            routingContext.response()
                    .putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                    .end();
        });
    }

    private String generateBase62Id() {
        final String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        long b10 = Math.abs(UUID.randomUUID().hashCode());
        String ret = "";
        while (b10 > 0) {
            ret = characters.charAt((int) (b10 % 62)) + ret;
            b10 /= 62;
        }
        return ret;
    }

    private Optional<String> getSObjectName(String query) {
        final StringTokenizer tokenizer = new StringTokenizer(query, " ");
        for (String token = tokenizer.nextToken(); tokenizer.hasMoreTokens(); token = tokenizer.nextToken()) {
            if ("FROM".equalsIgnoreCase(token) && tokenizer.hasMoreTokens()) {
                return of(tokenizer.nextToken());
            }
        }
        return empty();
    }

    private JsonObject addRecordAttributes(JsonObject record, String apiVersion, String sObjectName) {
        final String sObjectId = ofNullable(record.getString(FIELD_ID))
                .orElseThrow(() -> new RuntimeException(String.format("Record missing '%s' field: %s", FIELD_ID, record)));

        final JsonObject attributes = new JsonObject();
        attributes.put("type", sObjectName);
        attributes.put("url", "/services/data/" + apiVersion + "/sobjects/" + sObjectName + "/" + sObjectId);

        record.put("attributes", attributes);

        return record;
    }
}
