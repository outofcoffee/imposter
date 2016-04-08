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
import java.util.StringTokenizer;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginImpl extends ConfiguredPlugin<SfdcPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(SfdcPluginImpl.class);

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

            final String sObjectName = getSObjectName(query);
            if (null == sObjectName) {
                routingContext.fail(new RuntimeException(String.format("Could not determine SObject name from query: %s", query)));
                return;
            }

            final SfdcPluginConfig config = configs.stream()
                    .filter(sfdcPluginConfig -> sObjectName.equalsIgnoreCase(sfdcPluginConfig.getsObjectName()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException(String.format("Unable to find mock config for SObject: %s", sObjectName)));

            final JsonArray records = FileUtil.loadResponseAsJsonArray(imposterConfig, config);

            for (int i = 0; i < records.size(); i++) {
                final JsonObject record = records.getJsonObject(i);
                addRecordAttributes(record, apiVersion, config);
            }

            final JsonObject responseWrapper = new JsonObject();
            responseWrapper.put("done", true);
            responseWrapper.put("records", records);
            responseWrapper.put("totalSize", records.size());

            LOGGER.info("Sending {} records in response to query: {}", records.size(), query);

            final HttpServerResponse response = routingContext.response();

            ofNullable(config.getContentType())
                    .ifPresent(contentType -> response.putHeader("Content-Type", contentType));

            response.setStatusCode(HttpURLConnection.HTTP_OK)
                    .end(Buffer.buffer(responseWrapper.encodePrettily()));
        });

        // SObject handler
        configs.forEach(config -> {
            router.get("/services/data/:apiVersion/sobjects/" + config.getsObjectName() + "/:sObjectId")
                    .handler(routingContext -> {
                        final HttpServerResponse response = routingContext.response();

                        final String apiVersion = routingContext.request().getParam("apiVersion");
                        final String sObjectId = routingContext.request().getParam("sObjectId");

                        // find and filter records
                        final JsonArray records = FileUtil.loadResponseAsJsonArray(imposterConfig, config);
                        final JsonObject result = findSObjectById(sObjectId, records);

                        if (null != result) {
                            addRecordAttributes(result, apiVersion, config);

                        } else {
                            // no such record
                            LOGGER.error("{} SObject with ID: {} not found", config.getsObjectName(), sObjectId);
                            response.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND).end();
                            return;
                        }

                        LOGGER.info("Sending record with ID: {}", sObjectId);

                        ofNullable(config.getContentType())
                                .ifPresent(contentType -> response.putHeader("Content-Type", contentType));

                        response.setStatusCode(HttpURLConnection.HTTP_OK)
                                .end(Buffer.buffer(result.encodePrettily()));
                    });
        });
    }

    private String getSObjectName(String query) {
        String sObjectName = null;
        final StringTokenizer tokenizer = new StringTokenizer(query, " ");
        for (String token = null; tokenizer.hasMoreTokens(); token = tokenizer.nextToken()) {
            if ("FROM".equalsIgnoreCase(token)) {
                sObjectName = tokenizer.nextToken();
                break;
            }
        }
        return sObjectName;
    }

    private JsonObject findSObjectById(String sObjectId, JsonArray records) {
        JsonObject result = null;
        for (int i = 0; i < records.size(); i++) {
            final JsonObject currentRecord = records.getJsonObject(i);

            if (currentRecord.getString("Id").equalsIgnoreCase(sObjectId)) {
                result = currentRecord;
                break;
            }
        }
        return result;
    }

    private void addRecordAttributes(JsonObject record, String apiVersion, SfdcPluginConfig config) {
        final String sObjectId = ofNullable(record.getString("Id"))
                .orElseThrow(() -> new RuntimeException(String.format("Record missing 'Id' field: %s", record)));

        final JsonObject attributes = new JsonObject();
        attributes.put("type", config.getsObjectName());
        attributes.put("url", "/services/data/" + apiVersion + "/sobjects/" + config.getsObjectName() + "/" + sObjectId);

        record.put("attributes", attributes);
    }
}
