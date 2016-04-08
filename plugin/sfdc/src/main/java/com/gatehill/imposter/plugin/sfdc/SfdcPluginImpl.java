package com.gatehill.imposter.plugin.sfdc;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.util.FileUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

import static java.util.Optional.*;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginImpl extends ConfiguredPlugin<SfdcPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(SfdcPluginImpl.class);
    private static final String FIELD_ID = "Id";

    @Inject
    private ImposterConfig imposterConfig;

    private List<SfdcPluginConfig> configs;

    @Override
    protected Class<SfdcPluginConfig> getConfigClass() {
        return SfdcPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<SfdcPluginConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        // oauth handler
        router.post("/services/oauth2/token").handler(routingContext -> {
            LOGGER.info("Handling oauth request");

            final JsonObject authResponse = new JsonObject();
            authResponse.put("access_token", "dummyAccessToken");
            authResponse.put("instance_url", imposterConfig.getServerUrl().toString());
            routingContext.response().end(authResponse.encode());
        });

        // query handler
        router.get("/services/data/:apiVersion/query/").handler(routingContext -> {
            final String apiVersion = routingContext.request().getParam("apiVersion");

            // e.g. 'SELECT Name, Id from Account LIMIT 100'
            final String query = routingContext.request().getParam("q");

            final String sObjectName = getSObjectName(query)
                    .orElseThrow(() -> new RuntimeException(String.format("Could not determine SObject name from query: %s", query)));

            final SfdcPluginConfig config = configs.stream()
                    .filter(sfdcPluginConfig -> sObjectName.equalsIgnoreCase(sfdcPluginConfig.getsObjectName()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException(String.format("Unable to find mock config for SObject: %s", sObjectName)));

            // enrich records
            final JsonArray records = FileUtil.loadResponseAsJsonArray(imposterConfig, config);
            for (int i = 0; i < records.size(); i++) {
                addRecordAttributes(records.getJsonObject(i), apiVersion, config.getsObjectName());
            }

            final JsonObject responseWrapper = new JsonObject();
            responseWrapper.put("done", true);
            responseWrapper.put("records", records);
            responseWrapper.put("totalSize", records.size());

            LOGGER.info("Sending {} SObjects in response to query: {}", records.size(), query);

            final HttpServerResponse response = routingContext.response();

            ofNullable(config.getContentType())
                    .ifPresent(contentType -> response.putHeader("Content-Type", contentType));

            response.setStatusCode(HttpURLConnection.HTTP_OK)
                    .end(Buffer.buffer(responseWrapper.encodePrettily()));
        });

        // get SObject handler
        configs.forEach(config -> {
            router.get("/services/data/:apiVersion/sobjects/" + config.getsObjectName() + "/:sObjectId")
                    .handler(routingContext -> {
                        final String apiVersion = routingContext.request().getParam("apiVersion");
                        final String sObjectId = routingContext.request().getParam("sObjectId");

                        // find and enrich record
                        final Optional<JsonObject> result = findSObjectById(sObjectId,
                                FileUtil.loadResponseAsJsonArray(imposterConfig, config))
                                .map(r -> addRecordAttributes(r, apiVersion, config.getsObjectName()));

                        final HttpServerResponse response = routingContext.response();

                        if (result.isPresent()) {
                            LOGGER.info("Sending SObject with ID: {}", sObjectId);

                            ofNullable(config.getContentType())
                                    .ifPresent(contentType -> response.putHeader("Content-Type", contentType));

                            response.setStatusCode(HttpURLConnection.HTTP_OK)
                                    .end(Buffer.buffer(result.get().encodePrettily()));
                        } else {
                            // no such record
                            LOGGER.error("{} SObject with ID: {} not found", config.getsObjectName(), sObjectId);
                            response.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
                                    .end();
                        }
                    });
        });

        // create SObject handler
        router.post("/services/data/:apiVersion/sobjects/:sObjectName")
                .handler(routingContext -> {
                    final String sObjectName = routingContext.request().getParam("sObjectName");
                    final JsonObject sObject = routingContext.getBodyAsJson();

                    LOGGER.info("Received create request for {}: {}", sObjectName, sObject);

                    final JsonObject result = new JsonObject();

                    // Note: ID has to be lowercase, for some reason
                    result.put("id", generateBase62Id());
                    result.put("success", true);

                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(HttpURLConnection.HTTP_CREATED)
                            .end(Buffer.buffer(result.encodePrettily()));
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

    private Optional<JsonObject> findSObjectById(String sObjectId, JsonArray records) {
        for (int i = 0; i < records.size(); i++) {
            final JsonObject currentRecord = records.getJsonObject(i);
            if (currentRecord.getString(FIELD_ID).equalsIgnoreCase(sObjectId)) {
                return of(currentRecord);
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
