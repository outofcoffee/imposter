package io.gatehill.imposter.plugin.openapi;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.RequireModules;
import io.gatehill.imposter.plugin.ScriptedPlugin;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.plugin.openapi.service.OpenApiService;
import io.gatehill.imposter.plugin.openapi.util.OpenApiVersionUtil;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.MapUtil;
import io.swagger.models.Scheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@PluginInfo("openapi")
@RequireModules(OpenApiModule.class)
public class OpenApiPluginImpl extends ConfiguredPlugin<OpenApiPluginConfig> implements ScriptedPlugin<OpenApiPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(OpenApiPluginImpl.class);
    private static final Pattern PATH_PARAM_PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9._\\-]+)}");
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

    @Inject
    private ResponseService responseService;

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
        final List<OpenAPI> allSpecs = Lists.newArrayListWithExpectedSize(configs.size());

        // specification mock endpoints
        configs.forEach(config -> {
            final OpenAPI swagger = OpenApiVersionUtil.parseSpecification(config);

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
        router.get(COMBINED_SPECIFICATION_PATH).handler(AsyncUtil.handleRoute(imposterConfig, vertx, routingContext -> handleCombinedSpec(routingContext, allSpecs)));
        router.getWithRegex(SPECIFICATION_PATH + "$").handler(AsyncUtil.handleRoute(imposterConfig, vertx, routingContext -> routingContext.response().putHeader("Location", SPECIFICATION_PATH + "/").setStatusCode(HttpUtil.HTTP_MOVED_PERM).end()));
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
    private void handlePathOperations(Router router, OpenApiPluginConfig config, OpenAPI swagger, String path, PathItem pathConfig) {
        pathConfig.readOperationsMap().forEach((httpMethod, operation) -> {
            final String fullPath = buildPath(swagger, path, config);
            LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath);

            // convert an io.swagger.models.HttpMethod to an io.vertx.core.http.HttpMethod
            final HttpMethod method = HttpMethod.valueOf(httpMethod.name());
            router.route(method, fullPath).handler(buildHandler(config, operation));
        });
    }

    /**
     * Construct the path at which the example response will be served.
     *
     * @param swagger the OpenAPI specification
     * @param path    the mock path
     * @param config  the mock configuration
     * @return the path
     */
    private String buildPath(OpenAPI swagger, String path, OpenApiPluginConfig config) {
        String basePath;

        if (config.isUseServerPathAsBaseUrl()) {
            // Treat the mock server as substitute for 'the' server.
            // Note: OASv2 'basePath' is converted to OASv3 'server' entries.

            final Optional<Server> firstServer = swagger.getServers().stream().findFirst();
            if (firstServer.isPresent()) {
                final String url = ofNullable(firstServer.get().getUrl()).orElse("");
                if (url.length() > 1) {
                    // attempt to parse as URI and extract path
                    try {
                        basePath = new URI(url).getPath();
                    } catch (URISyntaxException ignored) {
                        basePath = "";
                    }
                } else {
                    basePath = "";
                }
            } else {
                basePath = "";
            }
        } else {
            basePath = "";
        }

        return basePath + convertPath(path);
    }

    /**
     * Returns an OpenAPI specification combining all the given specifications.
     *
     * @param routingContext the Vert.x routing context
     * @param allSpecs       all specifications
     */
    private void handleCombinedSpec(RoutingContext routingContext, List<OpenAPI> allSpecs) {
        try {
            final String combinedJson = specCache.get("combinedSpec", () -> {
                try {
                    final Scheme scheme = Scheme.forValue(imposterConfig.getPluginArgs().get(ARG_SCHEME));
                    final String basePath = imposterConfig.getPluginArgs().get(ARG_BASEPATH);
                    final String title = imposterConfig.getPluginArgs().get(ARG_TITLE);

                    final OpenAPI combined = openApiService.combineSpecifications(allSpecs, basePath, scheme, title);
                    return MapUtil.JSON_MAPPER.writeValueAsString(combined);

                } catch (JsonGenerationException e) {
                    throw new ExecutionException(e);
                }
            });

            routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
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
     * @param operation the specification operation  @return a route handler
     */
    private Handler<RoutingContext> buildHandler(OpenApiPluginConfig config, Operation operation) {
        return AsyncUtil.handleRoute(imposterConfig, vertx, routingContext -> {
            final HashMap<String, Object> context = newHashMap();
            context.put("operation", operation);

            scriptHandler(config, routingContext, getInjector(), context, responseBehaviour -> {
                final String statusCode = String.valueOf(responseBehaviour.getStatusCode());

                // look for a specification response based on the status code
                final Optional<ApiResponse> optionalMockResponse = operation.getResponses().entrySet().parallelStream()
                        .filter(mockResponse -> mockResponse.getKey().equals(statusCode))
                        .map(Map.Entry::getValue)
                        .findAny();

                // set status code regardless of response strategy
                final HttpServerResponse response = routingContext.response()
                        .setStatusCode(responseBehaviour.getStatusCode());

                if (optionalMockResponse.isPresent()) {
                    serveMockResponse(config, routingContext, responseBehaviour, optionalMockResponse.get());

                } else {
                    LOGGER.info("No explicit mock response found for URI {} with status code {}",
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
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     */
    private void serveMockResponse(OpenApiPluginConfig config,
                                   RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour,
                                   ApiResponse mockResponse) {

        responseService.sendResponse(config, config, routingContext, responseBehaviour, rc -> {
            // attempt to serve an example from the specification, falling back if not present
            serveExample(config, rc, responseBehaviour, mockResponse, this::fallback);
        });
    }

    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     * @param fallback          callback to invoke if no example was served
     */
    private void serveExample(OpenApiPluginConfig config,
                              RoutingContext routingContext,
                              ResponseBehaviour responseBehaviour,
                              ApiResponse mockResponse,
                              BiConsumer<RoutingContext, ResponseBehaviour> fallback) {

        final int statusCode = responseBehaviour.getStatusCode();

        // fetch all examples
        final Map<String, Object> examples = newHashMap();

        if (nonNull(mockResponse.getContent())) {
            mockResponse.getContent().forEach((mimeTypeName, mediaType) -> {
                if (nonNull(mediaType.getExample())) {
                    // "The example field is mutually exclusive of the examples field."
                    // https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#mediaTypeObject
                    examples.put(mimeTypeName, mediaType.getExample());
                } else if (nonNull(mediaType.getExamples())) {
                    mediaType.getExamples().forEach((exampleName, example) -> {
                        examples.put(mimeTypeName, example);
                    });
                }
            });
        }

        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in specification ({} candidates) for URI {} with status code {}",
                    examples.size(), routingContext.request().absoluteURI(), statusCode);

            final Optional<Map.Entry<String, Object>> example = findExample(
                    routingContext, config, examples);

            // serve example
            if (example.isPresent()) {
                final Map.Entry<String, Object> exampleEntry = example.get();

                final String exampleResponse = buildExampleResponse(exampleEntry);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Serving mock example for URI {} with status code {}: {}",
                            routingContext.request().absoluteURI(), statusCode, exampleResponse);
                } else {
                    LOGGER.info("Serving mock example for URI {} with status code {} (response body {} bytes)",
                            routingContext.request().absoluteURI(), statusCode,
                            ofNullable(exampleResponse).map(String::length).orElse(0));
                }

                // example key is its content type (should match one in the response 'provides' list)
                routingContext.response()
                        .putHeader(HttpUtil.CONTENT_TYPE, exampleEntry.getKey())
                        .end(exampleResponse);

                return;
            }
        }

        LOGGER.trace("No example matches found in specification for URI {} with status code {}",
                routingContext.request().absoluteURI(), statusCode);

        // no matching example - use fallback
        fallback.accept(routingContext, responseBehaviour);
    }

    /**
     * @param exampleEntry the example
     * @return the {@link String} representation of the example entry
     */
    private String buildExampleResponse(Map.Entry<String, Object> exampleEntry) {
        try {
            final Object exampleValue = exampleEntry.getValue();
            final String exampleResponse;
            if (exampleValue instanceof Example) {
                exampleResponse = ((Example) exampleValue).getValue().toString();
            } else if (exampleValue instanceof Map) {
                switch (exampleEntry.getKey()) {
                    case "application/json":
                        exampleResponse = MapUtil.JSON_MAPPER.writeValueAsString(exampleValue);
                        break;

                    case "text/x-yaml":
                    case "application/x-yaml":
                    case "application/yaml":
                        exampleResponse = MapUtil.YAML_MAPPER.writeValueAsString(exampleValue);
                        break;

                    default:
                        LOGGER.warn("Unsupported response MIME type - returning example object as string");
                        exampleResponse = exampleValue.toString();
                        break;
                }
            } else if (exampleValue instanceof String) {
                exampleResponse = (String) exampleValue;
            } else {
                LOGGER.warn("Unsupported example type - attempting String conversion");
                exampleResponse = exampleValue.toString();
            }
            return exampleResponse;

        } catch (JsonProcessingException e) {
            LOGGER.error("Error building example response", e);
            return "";
        }
    }

    /**
     * Locate an example, first by searching the matched content types, then, optionally, the first found.
     *
     * @param routingContext the Vert.x routing context
     * @param config         the plugin configuration
     * @param examples       the specification response examples
     */
    private Optional<Map.Entry<String, Object>> findExample(RoutingContext routingContext,
                                                            OpenApiPluginConfig config,
                                                            Map<String, Object> examples) {

        // the produced content types
        final Set<String> produces = Sets.newHashSet();
        produces.addAll(examples.keySet());

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
        LOGGER.warn("No example match found and no response file set for mock response for URI {} with status code {}" +
                " - sending empty response", routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        routingContext.response().end();
    }
}
