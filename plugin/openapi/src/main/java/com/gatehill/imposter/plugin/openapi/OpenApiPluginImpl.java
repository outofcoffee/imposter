package com.gatehill.imposter.plugin.openapi;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.ScriptedPlugin;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.plugin.openapi.service.OpenApiService;
import com.gatehill.imposter.script.ResponseBehaviour;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.MapUtil;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.models.*;
import io.swagger.parser.SwaggerParser;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.AsyncUtil.handleAsync;
import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginImpl extends ConfiguredPlugin<OpenApiPluginConfig> implements ScriptedPlugin<OpenApiPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(OpenApiPluginImpl.class);
    private static final Pattern PATH_PARAM_PLACEHOLDER = Pattern.compile("\\{([a-zA-Z]+)\\}");
    private static final String UI_WEB_ROOT = "swagger-ui";
    private static final String ARG_BASEPATH = "openapi.basepath";
    private static final String ARG_SCHEME = "openapi.scheme";
    private static final String ARG_TITLE = "openapi.title";
    static final String SPECIFICATION_PATH = "/_spec";
    static final String COMBINED_SPECIFICATION_PATH = SPECIFICATION_PATH + "/combined.json";

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private OpenApiService openApiService;

    private List<OpenApiPluginConfig> configs;

    /**
     * Holds the specifications.
     */
    private Cache<String, String> specCache = CacheBuilder.newBuilder().build();

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
        final List<Swagger> allSpecs = Lists.newArrayListWithExpectedSize(configs.size());

        // specification mock endpoints
        configs.forEach(config -> {
            final Swagger swagger = new SwaggerParser().read(Paths.get(
                    config.getParentDir().getAbsolutePath(), config.getSpecFile()).toString());

            if (null != swagger) {
                allSpecs.add(swagger);
                swagger.getPaths().forEach((path, pathConfig) ->
                        handlePathOperations(router, config, swagger, path, pathConfig));

            } else {
                throw new RuntimeException(String.format("Unable to load API specification: %s", config.getSpecFile()));
            }
        });

        // serve specification and UI
        LOGGER.debug("Adding specification UI at: {}", SPECIFICATION_PATH);
        router.get(COMBINED_SPECIFICATION_PATH).handler(handleAsync(routingContext -> handleCombinedSpec(routingContext, allSpecs)));
        router.getWithRegex(SPECIFICATION_PATH + "$").handler(handleAsync(routingContext -> routingContext.response().putHeader("Location", SPECIFICATION_PATH + "/").setStatusCode(HttpUtil.HTTP_MOVED_PERM).end()));
        router.get(SPECIFICATION_PATH + "/*").handler(StaticHandler.create(UI_WEB_ROOT));
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the Vert.x router
     * @param config     the plugin configuration
     * @param swagger    the OpenAPI specification
     * @param path       the mock path
     * @param pathConfig the path configuration
     */
    private void handlePathOperations(Router router, OpenApiPluginConfig config, Swagger swagger, String path, Path pathConfig) {
        pathConfig.getOperationMap().forEach((httpMethod, operation) -> {
            final String fullPath = ofNullable(swagger.getBasePath()).orElse("") + convertPath(path);
            LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath);

            // convert an {@link io.swagger.models.HttpMethod} to an {@link io.vertx.core.http.HttpMethod}
            final HttpMethod method = HttpMethod.valueOf(httpMethod.name());
            router.route(method, fullPath).handler(buildHandler(config, swagger, operation));
        });
    }

    /**
     * Returns an OpenAPI specification combining all the given specifications.
     *
     * @param routingContext the Vert.x routing context
     * @param allSpecs       all specifications
     */
    private void handleCombinedSpec(RoutingContext routingContext, List<Swagger> allSpecs) {
        try {
            final String combinedJson = specCache.get("combinedSpec", () -> {
                try {
                    final Scheme scheme = Scheme.forValue(imposterConfig.getPluginArgs().get(ARG_SCHEME));
                    final String basePath = imposterConfig.getPluginArgs().get(ARG_BASEPATH);
                    final String title = imposterConfig.getPluginArgs().get(ARG_TITLE);

                    final Swagger combined = openApiService.combineSpecifications(allSpecs, basePath, scheme, title);
                    return MapUtil.MAPPER.writeValueAsString(combined);

                } catch (JsonGenerationException e) {
                    throw new ExecutionException(e);
                }
            });

            routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .end(combinedJson);

        } catch (Exception e) {
            routingContext.fail(e);
        }
    }

    /**
     * Convert the OpenAPI path to a Vert.x path, including any parameter placeholders.
     *
     * @param path the OpenAPI path
     * @return the Vert.x path
     */
    private String convertPath(String path) {
        boolean matchFound;
        do {
            final Matcher matcher = PATH_PARAM_PLACEHOLDER.matcher(path);
            matchFound = matcher.find();
            if (matchFound) {
                path = matcher.replaceFirst(":" + matcher.group(1));
            }
        } while (matchFound);

        return path;
    }

    /**
     * Build a handler for the given operation.
     *
     * @param config    the plugin configuration
     * @param swagger   the OpenAPI specification
     * @param operation the specification operation  @return a route handler
     */
    private Handler<RoutingContext> buildHandler(OpenApiPluginConfig config, Swagger swagger, Operation operation) {
        return handleAsync(routingContext -> {
            final HashMap<String, Object> context = Maps.newHashMap();
            context.put("operation", operation);

            scriptHandler(config, routingContext, getInjector(), context, responseBehaviour -> {
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
                    serveMockResponse(config, swagger, operation, routingContext, responseBehaviour, optionalMockResponse.get());

                } else {
                    LOGGER.info("No explicit mock response found for URI {} and status code {}",
                            routingContext.request().absoluteURI(), statusCode);

                    response.end();
                }
            });
        });
    }

    /**
     * Build a response from the specification.
     *
     * @param config            the plugin configuration
     * @param swagger           the OpenAPI specification
     * @param operation         the specification operation
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     */
    private void serveMockResponse(OpenApiPluginConfig config, Swagger swagger, Operation operation,
                                   RoutingContext routingContext, ResponseBehaviour responseBehaviour,
                                   Response mockResponse) {

        LOGGER.trace("Found mock response for URI {} and status code {}",
                routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        if (!responseBehaviour.getResponseHeaders().isEmpty()) {
            responseBehaviour.getResponseHeaders().forEach((header, value) ->
                    routingContext.response().putHeader(header, value));
        }

        if (!Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
            // response file takes precedence
            serveResponseFile(config, routingContext, responseBehaviour);

        } else if (!Strings.isNullOrEmpty(responseBehaviour.getResponseData())) {
            // response data
            LOGGER.info("Response data is: {}", responseBehaviour.getResponseData());
            routingContext.response().putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .setStatusCode(responseBehaviour.getStatusCode()).end(responseBehaviour.getResponseData());

        } else {
            // attempt to serve an example from the specification, falling back if not present
            serveExample(config, swagger, routingContext, responseBehaviour, operation, mockResponse, this::fallback);
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

        response.sendFile(Paths.get(config.getParentDir().getAbsolutePath(),
                responseBehaviour.getResponseFile()).toString());
    }

    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param config            the plugin configuration
     * @param swagger           the OpenAPI specification
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param operation         the specification operation
     * @param mockResponse      the specification response
     * @param fallback          callback to invoke if no example was served
     */
    private void serveExample(OpenApiPluginConfig config, Swagger swagger, RoutingContext routingContext,
                              ResponseBehaviour responseBehaviour, Operation operation,
                              Response mockResponse, BiConsumer<RoutingContext, ResponseBehaviour> fallback) {

        final int statusCode = responseBehaviour.getStatusCode();

        @SuppressWarnings("unchecked") final Map<String, Object> examples = ofNullable(mockResponse.getExamples()).orElse(Collections.EMPTY_MAP);

        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in specification ({} candidates) for URI {} and status code {}",
                    examples.size(), routingContext.request().absoluteURI(), statusCode);

            final Optional<Map.Entry<String, Object>> example = findExample(
                    routingContext, config, swagger, operation, examples);

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
     * @param routingContext the Vert.x routing context
     * @param config         the plugin configuration
     * @param swagger        the OpenAPI specification
     * @param operation      the specification operation    @return an optional example entry
     * @param examples       the specification response examples
     */
    private Optional<Map.Entry<String, Object>> findExample(RoutingContext routingContext, OpenApiPluginConfig config,
                                                            Swagger swagger, Operation operation,
                                                            Map<String, Object> examples) {

        // consolidate the produced content types
        final Set<String> produces = Sets.newHashSet();
        ofNullable(swagger.getProduces()).ifPresent(produces::addAll);
        ofNullable(operation.getProduces()).ifPresent(produces::addAll);

        // match accepted content types to those produced by this response operation
        final List<String> matchedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext).parallelStream()
                .filter(produces::contains)
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
