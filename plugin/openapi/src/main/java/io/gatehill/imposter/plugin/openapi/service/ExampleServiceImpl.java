package io.gatehill.imposter.plugin.openapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.MapUtil;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ExampleServiceImpl implements ExampleService {
    private static final Logger LOGGER = LogManager.getLogger(ExampleServiceImpl.class);

    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     * @param fallback          callback to invoke if no example was served
     */
    @Override
    public void serveExample(OpenApiPluginConfig config,
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
}
