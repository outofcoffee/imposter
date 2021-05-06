package io.gatehill.imposter.plugin.openapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.swagger.v3.oas.models.examples.Example;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static io.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static io.gatehill.imposter.util.MapUtil.YAML_MAPPER;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * Serialises and transmits examples to the client.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseTransmissionServiceImpl implements ResponseTransmissionService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseTransmissionServiceImpl.class);

    @Override
    public <T> void transmitExample(RoutingContext routingContext, ContentTypedHolder<T> example) {
        final Object exampleValue = example.getValue();
        if (isNull(exampleValue)) {
            LOGGER.info("No example found - returning empty response");
            routingContext.response().end();
            return;
        }
        final String exampleResponse = buildExampleResponse(example.getContentType(), example.getValue());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Serving mock example for URI {} with status code {}: {}",
                    routingContext.request().absoluteURI(), routingContext.response().getStatusCode(), exampleResponse);
        } else {
            LOGGER.info("Serving mock example for URI {} with status code {} (response body {} bytes)",
                    routingContext.request().absoluteURI(), routingContext.response().getStatusCode(),
                    ofNullable(exampleResponse).map(String::length).orElse(0));
        }

        routingContext.response()
                .putHeader(CONTENT_TYPE, example.getContentType())
                .end(exampleResponse);
    }

    /**
     * Construct a response body from the example, based on the content type.
     *
     * @param contentType the content type
     * @param example     the example candidate - may be strongly typed {@link Example}, map, list, or raw
     * @return the {@link String} representation of the example entry
     */
    private String buildExampleResponse(String contentType, Object example) {
        final String exampleResponse;
        if (example instanceof Example) {
            exampleResponse = ((Example) example).getValue().toString();
        } else if (example instanceof List) {
            exampleResponse = serialiseList(contentType, (List<?>) example);
        } else if (example instanceof Map) {
            exampleResponse = serialise(contentType, example);
        } else if (example instanceof String) {
            exampleResponse = (String) example;
        } else {
            LOGGER.warn("Unsupported example type '{}' - attempting String conversion", example.getClass());
            exampleResponse = example.toString();
        }
        return exampleResponse;
    }

    /**
     * Serialises the list according to the content type.
     *
     * @param contentType the content type
     * @param example     a {@link List} to be serialised
     * @return the serialised list
     */
    private String serialiseList(String contentType, List<?> example) {
        final List<?> transformedList = transformListForSerialisation(example);
        return serialise(contentType, transformedList);
    }

    /**
     * Ensures each element can be serialised correctly as part of a list, allowing
     * for different list representations between serialisation formats.
     *
     * @param example the {@link List} whose elements to transform
     * @return the transformed list
     */
    private List<?> transformListForSerialisation(List<?> example) {
        return example.stream().map(e -> {
            if (e instanceof Example) {
                return ((Example) e).getValue().toString();
            } else if (e instanceof List) {
                return transformListForSerialisation((List<?>) e);
            } else if (e instanceof Map) {
                return e;
            } else if (e instanceof String) {
                return (String) e;
            } else {
                LOGGER.warn("Unsupported example element type '{}' - attempting String conversion", e.getClass());
                return e.toString();
            }
        }).collect(Collectors.toList());
    }

    /**
     * Serialises the object according to the content type.
     *
     * @param contentType the content type
     * @param example     an object to be serialised
     * @return the serialisation
     */
    private String serialise(String contentType, Object example) {
        try {
            final String exampleResponse;
            switch (contentType) {
                case "application/json":
                    exampleResponse = JSON_MAPPER.writeValueAsString(example);
                    break;

                case "text/x-yaml":
                case "application/x-yaml":
                case "application/yaml":
                    exampleResponse = YAML_MAPPER.writeValueAsString(example);
                    break;

                default:
                    LOGGER.warn("Unsupported response MIME type '{}' - returning example object as string", contentType);
                    exampleResponse = example.toString();
                    break;
            }
            return exampleResponse;

        } catch (JsonProcessingException e) {
            LOGGER.error("Error building example response", e);
            return "";
        }
    }
}
