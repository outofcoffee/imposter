/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * @author Pete Cornish
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
