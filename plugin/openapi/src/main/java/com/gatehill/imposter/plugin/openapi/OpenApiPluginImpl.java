package com.gatehill.imposter.plugin.openapi;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.ScriptedPlugin;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.util.HttpUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginImpl extends ConfiguredPlugin<OpenApiPluginConfig> implements ScriptedPlugin<OpenApiPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(OpenApiPluginImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    private List<OpenApiPluginConfig> configs;

    @Override
    protected Class<OpenApiPluginConfig> getConfigClass() {
        return OpenApiPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<OpenApiPluginConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        configs.forEach(config -> {
            final Swagger swagger = new SwaggerParser().read(Paths.get(
                    imposterConfig.getConfigDir(), config.getSpecFile()).toString());

            if (null != swagger) {
                // bind a handler to each path
                swagger.getPaths()
                        .forEach((path, pathConfig) -> pathConfig.getOperationMap()
                                .forEach((httpMethod, operation) -> {
                                    final String fullPath = ofNullable(swagger.getBasePath()).orElse("") + path;
                                    LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath);

                                    // convert an {@link io.swagger.models.HttpMethod} to an {@link io.vertx.core.http.HttpMethod}
                                    final HttpMethod method = HttpMethod.valueOf(httpMethod.name());
                                    router.route(method, fullPath).handler(buildHandler(config, operation));
                                }));

            } else {
                throw new RuntimeException(String.format("Unable to load API specification: %s", config.getSpecFile()));
            }
        });
    }

    /**
     * Build a handler for the given operation.
     *
     * @param config    the plugin configuration
     * @param operation the specification operation
     * @return a route handler
     */
    private Handler<RoutingContext> buildHandler(OpenApiPluginConfig config, Operation operation) {
        return routingContext -> {
            final HashMap<String, Object> context = Maps.newHashMap();
            context.put("operation", operation);

            scriptHandler(config, routingContext, context, responseBehaviour -> {
                final String statusCode = String.valueOf(responseBehaviour.getStatusCode());

                // look for a specification response based on the status code
                final Optional<Response> optionalMockResponse = operation.getResponses().entrySet().parallelStream()
                        .filter(mockResponse -> mockResponse.getKey().equals(statusCode))
                        .map(Map.Entry::getValue)
                        .findAny();

                // set status code regardless of response strategy
                final HttpServerResponse response = routingContext.response()
                        .setStatusCode(responseBehaviour.getStatusCode());

                if (optionalMockResponse.isPresent()) {
                    serveMockResponse(config, operation, routingContext, responseBehaviour, optionalMockResponse.get());

                } else {
                    LOGGER.info("No explicit mock response found for URI {} and status code {}",
                            routingContext.request().absoluteURI(), statusCode);

                    response.end();
                }
            });
        };
    }

    /**
     * Build a response from the specification.
     *
     * @param config            the plugin configuration
     * @param operation         the specification operation
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     */
    private void serveMockResponse(OpenApiPluginConfig config, Operation operation, RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour, Response mockResponse) {

        LOGGER.trace("Found mock response for URI {} and status code {}",
                routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        if (!Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
            // response file takes precedence
            serveResponseFile(config, routingContext, responseBehaviour);

        } else {
            // attempt to serve an example from the specification, falling back if not present
            serveExample(config, routingContext, responseBehaviour, operation, mockResponse, this::fallback);
        }
    }

    /**
     * Reply with a static response file.
     *
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private void serveResponseFile(OpenApiPluginConfig config, RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour) {

        LOGGER.debug("Serving response file {} for URI {} and status code {}",
                responseBehaviour.getResponseFile(),
                routingContext.request().absoluteURI(),
                responseBehaviour.getStatusCode());

        final HttpServerResponse response = routingContext.response();

        // explicit content type
        if (!Strings.isNullOrEmpty(config.getContentType())) {
            response.putHeader(CONTENT_TYPE, config.getContentType());
        }

        response.sendFile(Paths.get(imposterConfig.getConfigDir(), responseBehaviour.getResponseFile()).toString());
    }

    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param operation         the specification operation
     * @param mockResponse      the specification response
     * @param fallback          callback to invoke if no example was served
     */
    private void serveExample(OpenApiPluginConfig config, RoutingContext routingContext,
                              ResponseBehaviour responseBehaviour, Operation operation,
                              Response mockResponse, BiConsumer<RoutingContext, ResponseBehaviour> fallback) {

        final int statusCode = responseBehaviour.getStatusCode();

        @SuppressWarnings("unchecked")
        final Map<String, Object> examples = ofNullable(mockResponse.getExamples()).orElse(Collections.EMPTY_MAP);

        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in specification ({} candidates) for URI {} and status code {}",
                    examples.size(), routingContext.request().absoluteURI(), statusCode);

            final Optional<Map.Entry<String, Object>> example = findExample(config, examples, routingContext, operation);

            // serve example
            if (example.isPresent()) {
                final Map.Entry<String, Object> exampleEntry = example.get();

                final String exampleResponse = exampleEntry.getValue().toString();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Serving mock example for URI {} and status code {}: {}",
                            routingContext.request().absoluteURI(), statusCode, exampleResponse);
                } else {
                    LOGGER.info("Serving mock example for URI {} and status code {} (response body {} bytes)",
                            routingContext.request().absoluteURI(), statusCode,
                            ofNullable(exampleResponse).map(String::length).orElse(0));
                }

                // example key is its content type (should match one in the response 'provides' list)
                routingContext.response()
                        .putHeader(CONTENT_TYPE, exampleEntry.getKey())
                        .end(exampleResponse);

                return;
            }
        }

        LOGGER.trace("No example matches found in specification for URI {} and status code {}",
                routingContext.request().absoluteURI(), statusCode);

        // no matching example - use fallback
        fallback.accept(routingContext, responseBehaviour);
    }

    /**
     * Locate an example, first by searching the matched content types, then, optionally, the first found.
     *
     * @param config         the plugin configuration
     * @param examples       the specification response examples
     * @param routingContext the Vert.x routing context
     * @param operation      the specification operation
     * @return an optional example entry
     */
    private Optional<Map.Entry<String, Object>> findExample(OpenApiPluginConfig config, Map<String, Object> examples,
                                                            RoutingContext routingContext, Operation operation) {

        // match accepted content types to those produced by this response operation
        final List<String> matchedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext).parallelStream()
                .filter(a -> operation.getProduces().contains(a))
                .collect(Collectors.toList());

        // match first example by produced and accepted content types
        if (matchedContentTypes.size() > 0) {
            final Optional<Map.Entry<String, Object>> firstMatchingExample = examples.entrySet().parallelStream()
                    .filter(example -> matchedContentTypes.contains(example.getKey()))
                    .findFirst();

            if (firstMatchingExample.isPresent()) {
                final Map.Entry<String, Object> example = firstMatchingExample.get();
                LOGGER.debug("Exact example match found ({}) from specification", example.getKey());

                return Optional.of(example);
            }
        }

        // fallback to first example found
        if (config.isPickFirstIfNoneMatch()) {
            final Map.Entry<String, Object> example = examples.entrySet().iterator().next();
            LOGGER.debug("No exact example match found - choosing one example ({}) from specification." +
                    " You can switch off this behaviour by setting configuration option: pickFirstIfNoneMatch=false", example.getKey());

            return Optional.of(example);
        }

        // no matching example
        return empty();
    }

    /**
     * Handles the scenario when no example is found.
     *
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private void fallback(RoutingContext routingContext, ResponseBehaviour responseBehaviour) {
        LOGGER.warn("No example match found and no response file set for mock response for URI {} and status code {}" +
                " - sending empty response", routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        routingContext.response().end();
    }
}
