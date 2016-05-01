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
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
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
            final Swagger swagger = new SwaggerParser().read(config.getSpecFile());

            swagger.getPaths().forEach((path, pathConfig) -> pathConfig.getOperationMap().forEach((httpMethod, operation) -> {
                router.route(convertMethod(httpMethod), path).handler(buildHandler(config, operation));
            }));
        });
    }

    private HttpMethod convertMethod(io.swagger.models.HttpMethod httpMethod) {
        return HttpMethod.valueOf(httpMethod.name());
    }

    private Handler<RoutingContext> buildHandler(OpenApiPluginConfig config, Operation operation) {
        return routingContext -> {
            final HashMap<String, Object> context = Maps.newHashMap();
            context.put("operation", operation);

            scriptHandler(config, routingContext, context, responseBehaviour -> {
                final String statusCode = String.valueOf((responseBehaviour.getStatusCode()));

                final Optional<Response> optionalMockResponse = operation.getResponses().entrySet().parallelStream()
                        .filter(r -> r.getKey().equals(statusCode))
                        .map(Map.Entry::getValue)
                        .findAny();

                final HttpServerResponse response = routingContext.response()
                        .setStatusCode(responseBehaviour.getStatusCode());

                if (!optionalMockResponse.isPresent()) {
                    LOGGER.debug("No explicit mock response found at URI {} and status code {}",
                            routingContext.request().absoluteURI(), statusCode);

                    response.end();

                } else {
                    LOGGER.trace("Found mock response for URI {} and status code {}",
                            routingContext.request().absoluteURI(), statusCode);

                    if (!Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                        // response file takes precedence
                        serveResponseFile(config, routingContext, responseBehaviour, statusCode, response);

                    } else {
                        // look for example
                        final Response mockResponse = optionalMockResponse.get();
                        final boolean exampleServed = attemptServeExample(routingContext, operation, statusCode,
                                response, mockResponse);

                        if (!exampleServed) {
                            // no example found
                            LOGGER.debug("No example found and no response file set for mock response at URI {} and status code {}",
                                    routingContext.request().absoluteURI(), statusCode);

                            response.end();
                        }
                    }
                }
            });
        };
    }

    private void serveResponseFile(OpenApiPluginConfig config, RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour, String statusCode, HttpServerResponse response) {

        LOGGER.debug("Serving response file {} for URI {} and status code {}",
                responseBehaviour.getResponseFile(), routingContext.request().absoluteURI(), statusCode);

        // explicit content type
        if (!Strings.isNullOrEmpty(config.getContentType())) {
            response.putHeader(CONTENT_TYPE, config.getContentType());
        }

        response.sendFile(Paths.get(imposterConfig.getConfigDir(),
                responseBehaviour.getResponseFile()).toString());
    }

    private boolean attemptServeExample(RoutingContext routingContext, Operation operation, String statusCode,
                                        HttpServerResponse response, Response mockResponse) {

        @SuppressWarnings("unchecked")
        final Map<String, Object> examples = ofNullable(mockResponse.getExamples()).orElse(Collections.EMPTY_MAP);

        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in {} candidates for URI {} and status code {}",
                    examples.size(), routingContext.request().absoluteURI(), statusCode);

            // match accepted content types to those produced by this response operation
            final List<String> matchedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext).parallelStream()
                    .filter(a -> operation.getProduces().contains(a))
                    .collect(Collectors.toList());

            // match first example by produced and accepted content types
            if (matchedContentTypes.size() > 0) {
                final Optional<Map.Entry<String, Object>> firstMatchingExample = examples.entrySet().parallelStream()
                        .filter(example -> matchedContentTypes.contains(example.getKey()))
                        .findFirst();

                // serve example
                if (firstMatchingExample.isPresent()) {
                    final String exampleResponse = firstMatchingExample.get().getValue().toString();

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Serving mock example for URI {} and status code {}: {}",
                                routingContext.request().absoluteURI(), statusCode, exampleResponse);
                    } else {
                        LOGGER.debug("Serving mock example for URI {} and status code {} (response body {} bytes)",
                                routingContext.request().absoluteURI(), statusCode,
                                ofNullable(exampleResponse).map(String::length).orElse(0));
                    }

                    // example key is its content type (should match one in the response 'provides' list)
                    response.putHeader(CONTENT_TYPE, firstMatchingExample.get().getKey());

                    response.end(exampleResponse);
                    return true;
                }
            }

        } else {
            LOGGER.trace("No mock examples for URI {} and status code {}",
                    routingContext.request().absoluteURI(), statusCode);
        }

        // no matching example
        return false;
    }
}
