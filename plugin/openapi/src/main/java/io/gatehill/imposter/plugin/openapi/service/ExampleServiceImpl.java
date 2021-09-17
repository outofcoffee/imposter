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

import com.google.common.base.Strings;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.gatehill.imposter.plugin.openapi.util.RefUtil;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.util.HttpUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ExampleServiceImpl implements ExampleService {
    private static final Logger LOGGER = LogManager.getLogger(ExampleServiceImpl.class);

    @Inject
    private SchemaService schemaService;

    @Inject
    private ResponseTransmissionService responseTransmissionService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean serveExample(
            ImposterConfig imposterConfig,
            OpenApiPluginConfig config,
            RoutingContext routingContext,
            ResponseBehaviour responseBehaviour,
            ApiResponse specResponse,
            OpenAPI spec
    ) {
        final Optional<Content> optionalContent = findContent(spec, specResponse);
        if (optionalContent.isPresent()) {
            final Content responseContent = optionalContent.get();

            final Optional<ContentTypedHolder<Object>> inlineExample = findInlineExample(
                    config, routingContext, responseBehaviour, responseContent
            );
            if (inlineExample.isPresent()) {
                responseTransmissionService.transmitExample(routingContext, inlineExample.get());
                return true;

            } else {
                LOGGER.trace("No inline examples found; checking schema");
                final Optional<ContentTypedHolder<Schema<?>>> schema = findResponseSchema(
                        config, routingContext, responseContent
                );
                if (schema.isPresent()) {
                    return serveFromSchema(routingContext, spec, schema.get());
                }
            }
        }

        LOGGER.debug("No example matches found in specification for URI {} matching status code {}",
                routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        // no matching example
        return false;
    }

    private Optional<Content> findContent(OpenAPI spec, ApiResponse response) {
        // $ref takes precedence, per spec:
        //   "Any sibling elements of a $ref are ignored. This is because
        //   $ref works by replacing itself and everything on its level
        //   with the definition it is pointing at."
        // See: https://swagger.io/docs/specification/using-ref/
        if (nonNull(response.get$ref())) {
            LOGGER.trace("Using response from component reference: {}", response.get$ref());
            final ApiResponse resolvedResponse = RefUtil.lookupResponseRef(spec, response);
            return ofNullable(resolvedResponse.getContent());
        } else if (nonNull(response.getContent())) {
            LOGGER.trace("Using inline response");
            return of(response.getContent());
        } else {
            return empty();
        }
    }

    private Optional<ContentTypedHolder<Object>> findInlineExample(
            OpenApiPluginConfig config,
            RoutingContext routingContext,
            ResponseBehaviour responseBehaviour,
            Content responseContent
    ) {
        final List<ResponseEntities<Object>> examples = newArrayList();

        // fetch all examples
        responseContent.forEach((mimeTypeName, mediaType) -> {
            // Example field takes precedence, per spec:
            //  "The example field is mutually exclusive of the examples field."
            // See: https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#mediaTypeObject
            if (nonNull(mediaType.getExample())) {
                examples.add(ResponseEntities.of("inline example", mimeTypeName, mediaType.getExample()));
            } else if (nonNull(mediaType.getExamples())) {
                mediaType.getExamples().forEach((exampleName, example) -> {
                    examples.add(ResponseEntities.of("inline example", mimeTypeName, example, exampleName));
                });
            }
        });

        final Optional<ContentTypedHolder<Object>> example;
        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in specification ({} candidates) for URI {}",
                    examples.size(), routingContext.request().absoluteURI());
            example = matchExample(routingContext, config, responseBehaviour, examples);

        } else {
            example = empty();
        }
        return example;
    }

    private Optional<ContentTypedHolder<Schema<?>>> findResponseSchema(
            OpenApiPluginConfig config,
            RoutingContext routingContext,
            Content responseContent
    ) {
        final List<ResponseEntities<Schema<?>>> schemas = newArrayList();
        responseContent.forEach((mimeTypeName, mediaType) -> {
            if (nonNull(mediaType.getSchema())) {
                schemas.add(ResponseEntities.of("response schema", mimeTypeName, mediaType.getSchema()));
            }
        });
        return matchByContentType(routingContext, config, schemas);
    }

    /**
     * Locate an item of type {@link T}, first by searching the matched examples by name,
     * then by content type, then, optionally, falling back to the first found.
     *
     * @param routingContext    the Vert.x routing context
     * @param config            the plugin configuration
     * @param responseBehaviour the response behaviour
     * @param entriesToSearch   the examples
     * @return an optional, containing the object for the given content type
     */
    private <T> Optional<ContentTypedHolder<T>> matchExample(
            RoutingContext routingContext,
            OpenApiPluginConfig config,
            ResponseBehaviour responseBehaviour,
            List<ResponseEntities<T>> entriesToSearch
    ) {
        // a specific example has been selected
        if (!Strings.isNullOrEmpty(responseBehaviour.getExampleName())) {
            final Optional<ResponseEntities<T>> candidateExample = entriesToSearch.stream()
                    .filter(entry -> responseBehaviour.getExampleName().equals(entry.name))
                    .findFirst();

            if (candidateExample.isPresent()) {
                LOGGER.debug("Exact example selected: {}", responseBehaviour.getExampleName());
                final ResponseEntities<T> responseEntities = candidateExample.get();
                return convertToContentTypedExample(responseEntities);
            } else {
                LOGGER.warn("No example named '{}' was present", responseBehaviour.getExampleName());
            }
        }

        return matchByContentType(routingContext, config, entriesToSearch);
    }

    /**
     * Locate an entity of type {@link T}, first by searching the matched content types, then, optionally,
     * falling back to the first found.
     *
     * @param routingContext  the Vert.x routing context
     * @param config          the plugin configuration
     * @param entriesToSearch the examples
     * @return an optional, containing the object for the given content type
     */
    private <T> Optional<ContentTypedHolder<T>> matchByContentType(
            RoutingContext routingContext,
            OpenApiPluginConfig config,
            List<ResponseEntities<T>> entriesToSearch
    ) {
        // the produced content types
        final Set<String> produces = entriesToSearch.stream()
                .map(entry -> entry.contentType)
                .collect(Collectors.toSet());

        // match accepted content types to those produced by this response operation
        final List<String> matchedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext).stream()
                .filter(produces::contains)
                .collect(Collectors.toList());

        // match example by produced and accepted content types, assuming example name is a content type
        if (matchedContentTypes.size() > 0) {
            final List<ResponseEntities<T>> candidateEntities = entriesToSearch.stream()
                    .filter(example -> matchedContentTypes.contains(example.contentType))
                    .collect(Collectors.toList());

            if (!candidateEntities.isEmpty()) {
                final ResponseEntities<T> entity = candidateEntities.get(0);

                if (candidateEntities.size() > 1) {
                    LOGGER.warn("More than one {} found matching accepted content types ({}), but no example name specified. Selecting first entry: {}", entity.entityDescription, matchedContentTypes, entity.contentType);
                } else {
                    LOGGER.debug("Exact {} match found for accepted content type ({}) from specification", entity.entityDescription, entity.contentType);
                }

                return convertToContentTypedExample(entity);
            }
        }

        // fallback to first example found
        if (config.isPickFirstIfNoneMatch() && !entriesToSearch.isEmpty()) {
            final ResponseEntities<T> example = entriesToSearch.iterator().next();
            LOGGER.debug("No exact match found for accepted content types - choosing first item found ({}) from specification." +
                    " You can switch off this behaviour by setting configuration option 'pickFirstIfNoneMatch: false'", example.contentType);

            return convertToContentTypedExample(example);
        }

        // no matching example
        return empty();
    }

    private static <T> Optional<ContentTypedHolder<T>> convertToContentTypedExample(ResponseEntities<T> entry) {
        return of(new ContentTypedHolder<T>(entry.contentType, entry.item));
    }

    private boolean serveFromSchema(RoutingContext routingContext, OpenAPI spec, ContentTypedHolder<Schema<?>> schema) {
        try {
            final ContentTypedHolder<?> dynamicExamples = schemaService.collectExamples(routingContext.request(), spec, schema);
            responseTransmissionService.transmitExample(routingContext, dynamicExamples);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error serving example from schema", e);
            return false;
        }
    }

    private static class ResponseEntities<T> {
        public String entityDescription;
        public String name;
        public T item;
        public String contentType;

        private ResponseEntities(String entityDescription, String contentType, T item, String name) {
            this.entityDescription = entityDescription;
            this.contentType = contentType;
            this.item = item;
            this.name = name;
        }

        public static <T> ResponseEntities<T> of(String entityDescription, String contentType, T item) {
            return of(entityDescription, contentType, item, null);
        }

        public static <T> ResponseEntities<T> of(String entityDescription, String contentType, T item, String name) {
            return new ResponseEntities<>(entityDescription, contentType, item, name);
        }
    }
}
