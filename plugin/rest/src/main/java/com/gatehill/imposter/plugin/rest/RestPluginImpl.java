package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.ScriptedPlugin;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.plugin.config.ResourceConfig;
import com.gatehill.imposter.service.ResponseService;
import com.gatehill.imposter.util.FileUtil;
import com.gatehill.imposter.util.HttpUtil;
import com.google.common.base.Strings;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gatehill.imposter.util.AsyncUtil.handleRoute;
import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON;
import static java.util.Optional.ofNullable;

/**
 * Plugin for simple RESTful APIs.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginImpl<C extends RestPluginConfig> extends ConfiguredPlugin<C> implements ScriptedPlugin<ResourceConfig> {
    private static final Logger LOGGER = LogManager.getLogger(RestPluginImpl.class);

    /**
     * Example: <pre>/anything/:id/something</pre>
     */
    private static final Pattern PARAM_MATCHER = Pattern.compile(".*:(.+).*");

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private ResponseService responseService;

    private List<C> configs;

    @SuppressWarnings("unchecked")
    @Override
    protected Class<C> getConfigClass() {
        return (Class<C>) RestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<C> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        configs.forEach(config -> {
            // add root handler
            addObjectHandler(router, "", config, config.getContentType());

            // add child resource handlers
            ofNullable(config.getResources()).ifPresent(resources -> resources.forEach(resource ->
                    addResourceHandler(router, config, resource, config.getContentType())));
        });
    }

    private void addResourceHandler(Router router, C rootConfig, RestResourceConfig resourceConfig, String contentType) {
        final ResourceConfigType resourceType = ofNullable(resourceConfig.getType()).orElse(ResourceConfigType.OBJECT);
        switch (resourceType) {
            case OBJECT:
                addObjectHandler(router, rootConfig, resourceConfig, contentType);
                break;

            case ARRAY:
                addArrayHandler(router, rootConfig, resourceConfig, contentType);
                break;
        }
    }

    private void addObjectHandler(Router router, RestPluginConfig rootConfig, ResourceConfig resourceConfig, String contentType) {
        addObjectHandler(router, rootConfig.getPath(), resourceConfig, contentType);
    }

    private void addObjectHandler(Router router, String rootPath, ResourceConfig resourceConfig, String contentType) {
        final String qualifiedPath = buildQualifiedPath(rootPath, resourceConfig);
        LOGGER.debug("Adding REST object handler: {}", qualifiedPath);

        router.get(qualifiedPath).handler(handleRoute(imposterConfig, vertx, routingContext -> {
            // script should fire first
            scriptHandler(resourceConfig, routingContext, getInjector(), responseBehaviour -> {
                LOGGER.info("Handling object request for: {}", routingContext.request().absoluteURI());

                final HttpServerResponse response = routingContext.response();

                // add content type
                ofNullable(contentType).ifPresent(ct -> response.putHeader(HttpUtil.CONTENT_TYPE, ct));

                try {
                    response.setStatusCode(responseBehaviour.getStatusCode());

                    if (!Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                        LOGGER.info("Responding with file: {}", responseBehaviour.getResponseFile());
                        response.sendFile(Paths.get(resourceConfig.getParentDir().getAbsolutePath(),
                                responseBehaviour.getResponseFile()).toString());

                    } else if (!Strings.isNullOrEmpty(responseBehaviour.getResponseData())) {
                        // response data
                        LOGGER.info("Response data is: {}", responseBehaviour.getResponseData());
                        if (!response.headers().contains(CONTENT_TYPE)) {
                            LOGGER.debug("Guessing JSON content type");
                            response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);
                        }
                        response.end(responseBehaviour.getResponseData());

                    } else {
                        LOGGER.info("Response file and data are blank - returning empty response");
                        response.end();
                    }

                } catch (Exception e) {
                    routingContext.fail(e);
                }
            });
        }));
    }

    private String buildQualifiedPath(String rootPath, ResourceConfig resourceConfig) {
        final String qualifiedPath = ofNullable(rootPath).orElse("") + ofNullable(resourceConfig.getPath()).orElse("");
        return qualifiedPath.startsWith("/") ? qualifiedPath : "/" + qualifiedPath;
    }

    private void addArrayHandler(Router router, RestPluginConfig rootConfig, ResourceConfig resourceConfig, String contentType) {
        final String resourcePath = resourceConfig.getPath();
        final String qualifiedPath = buildQualifiedPath(rootConfig.getPath(), resourceConfig);
        LOGGER.debug("Adding REST array handler: {}", qualifiedPath);

        // validate path includes parameter
        final Matcher matcher = PARAM_MATCHER.matcher(resourcePath);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Resource '%s' does not contain a field ID parameter",
                    resourcePath));
        }

        router.get(qualifiedPath).handler(handleRoute(imposterConfig, vertx, routingContext -> {
            // script should fire first
            scriptHandler(resourceConfig, routingContext, getInjector(), responseBehaviour -> {
                LOGGER.info("Handling array request for: {}", routingContext.request().absoluteURI());

                // get the first param in the path
                final String idFieldName = matcher.group(1);
                final String idField = routingContext.request().getParam(idFieldName);

                // find row
                final Optional<JsonObject> result = FileUtil.findRow(idFieldName, idField,
                        responseService.loadResponseAsJsonArray(rootConfig, responseBehaviour));

                final HttpServerResponse response = routingContext.response();

                // add content type
                ofNullable(contentType).ifPresent(ct -> response.putHeader(HttpUtil.CONTENT_TYPE, ct));

                if (result.isPresent()) {
                    LOGGER.info("Returning single row for {}={}", idFieldName, idField);
                    response.setStatusCode(HttpUtil.HTTP_OK)
                            .end(result.get().encodePrettily());
                } else {
                    // no such record
                    LOGGER.error("No row found for {}={}", idFieldName, idField);
                    response.setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                            .end();
                }
            });
        }));
    }
}
