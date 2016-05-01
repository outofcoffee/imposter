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
            addObjectHandler(router, config, "", config.getContentType());

            // add child resource handlers
            ofNullable(config.getResources())
                    .ifPresent(resources -> resources
                            .forEach(resource -> addResourceHandler(router, config, resource, config.getContentType())));
        });
    }

    private void addResourceHandler(Router router, C rootConfig, RestResourceConfig resourceConfig, String contentType) {
        switch (resourceConfig.getType()) {
            case OBJECT:
                addObjectHandler(router, resourceConfig, rootConfig.getPath(), contentType);
                break;

            case ARRAY:
                addArrayHandler(router, resourceConfig, rootConfig.getPath(), contentType);
                break;
        }
    }

    private void addObjectHandler(Router router, ResourceConfig config, String basePath, String contentType) {
        router.get(basePath + config.getPath()).handler(routingContext -> {
            // script should fire first
            scriptHandler(config, routingContext, responseBehaviour -> {
                LOGGER.info("Handling object request for: {}", routingContext.request().absoluteURI());

                final HttpServerResponse response = routingContext.response();

                // add content type
                ofNullable(contentType).ifPresent(ct -> response.putHeader(HttpUtil.CONTENT_TYPE, ct));

                try {
                    response.setStatusCode(responseBehaviour.getStatusCode());

                    if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                        LOGGER.info("Response file blank - returning empty response");
                        response.end();

                    } else {
                        LOGGER.info("Responding with file: {}", responseBehaviour.getResponseFile());
                        response.sendFile(Paths.get(imposterConfig.getConfigDir(),
                                responseBehaviour.getResponseFile()).toString());
                    }

                } catch (Exception e) {
                    routingContext.fail(e);
                }
            });
        });
    }

    private void addArrayHandler(Router router, ResourceConfig config, String basePath, String contentType) {
        final String resourcePath = config.getPath();

        // validate path includes parameter
        final Matcher matcher = PARAM_MATCHER.matcher(resourcePath);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Resource '%s' does not contain a field ID parameter",
                    resourcePath));
        }

        router.get(basePath + resourcePath).handler(routingContext -> {
            // script should fire first
            scriptHandler(config, routingContext, responseBehaviour -> {
                LOGGER.info("Handling array request for: {}", routingContext.request().absoluteURI());

                // get the first param in the path
                final String idFieldName = matcher.group(1);
                final String idField = routingContext.request().getParam(idFieldName);

                // find row
                final Optional<JsonObject> result = FileUtil.findRow(idFieldName, idField,
                        responseService.loadResponseAsJsonArray(responseBehaviour));

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
        });
    }
}
