package io.gatehill.imposter.plugin.openapi.service;

import com.google.common.collect.Sets;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
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
            ApiResponse mockResponse,
            OpenAPI spec
    ) {
        final Optional<Content> optionalContent = findContent(spec, mockResponse);
        if (optionalContent.isPresent()) {
            final Content responseContent = optionalContent.get();

            final Optional<ContentTypedHolder<Object>> inlineExample = findInlineExample(config, routingContext, responseContent);
            if (inlineExample.isPresent()) {
                responseTransmissionService.transmitExample(routingContext, inlineExample.get());
                return true;

            } else {
                LOGGER.debug("No inline examples found; checking schema");
                final Optional<ContentTypedHolder<Schema<?>>> schema = findResponseSchema(config, routingContext, responseContent);
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
            Content responseContent
    ) {
        final Map<String, Object> examples = newHashMap();

        // fetch all examples
        responseContent.forEach((mimeTypeName, mediaType) -> {
            // Example field takes precedence, per spec:
            //  "The example field is mutually exclusive of the examples field."
            // See: https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#mediaTypeObject
            if (nonNull(mediaType.getExample())) {
                examples.put(mimeTypeName, mediaType.getExample());
            } else if (nonNull(mediaType.getExamples())) {
                mediaType.getExamples().forEach((exampleName, example) -> {
                    examples.put(mimeTypeName, example);
                });
            }
        });

        final Optional<ContentTypedHolder<Object>> example;
        if (examples.size() > 0) {
            LOGGER.trace("Checking for mock example in specification ({} candidates) for URI {}",
                    examples.size(), routingContext.request().absoluteURI());

            example = matchByContentType(routingContext, config, examples);
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
        final Map<String, Schema<?>> schemas = newHashMap();
        responseContent.forEach((mimeTypeName, mediaType) -> {
            if (nonNull(mediaType.getSchema())) {
                schemas.put(mimeTypeName, mediaType.getSchema());
            }
        });
        return matchByContentType(routingContext, config, schemas);
    }

    /**
     * Locate a map entry of type {@link T}, first by searching the matched content types, then, optionally, the first found.
     *
     * @param routingContext  the Vert.x routing context
     * @param config          the plugin configuration
     * @param entriesToSearch the entries, keyed by content type
     * @return an optional, containing the object for the given content type
     */
    private <T> Optional<ContentTypedHolder<T>> matchByContentType(
            RoutingContext routingContext,
            OpenApiPluginConfig config,
            Map<String, T> entriesToSearch
    ) {
        // the produced content types
        final Set<String> produces = Sets.newHashSet();
        produces.addAll(entriesToSearch.keySet());

        // match accepted content types to those produced by this response operation
        final List<String> matchedContentTypes = HttpUtil.readAcceptedContentTypes(routingContext).parallelStream()
                .filter(produces::contains)
                .collect(Collectors.toList());

        // match first example by produced and accepted content types
        if (matchedContentTypes.size() > 0) {
            final Optional<Map.Entry<String, T>> firstMatchingExample = entriesToSearch.entrySet().parallelStream()
                    .filter(example -> matchedContentTypes.contains(example.getKey()))
                    .findFirst();

            if (firstMatchingExample.isPresent()) {
                final Map.Entry<String, T> example = firstMatchingExample.get();
                LOGGER.debug("Exact example match found for content type ({}) from specification", example.getKey());

                return convertMapEntryToContentTypedExample(example);
            }
        }

        // fallback to first example found
        if (config.isPickFirstIfNoneMatch()) {
            final Map.Entry<String, T> example = entriesToSearch.entrySet().iterator().next();
            LOGGER.debug("No exact example match found for content type - choosing one example ({}) from specification." +
                    " You can switch off this behaviour by setting configuration option: pickFirstIfNoneMatch=false", example.getKey());

            return convertMapEntryToContentTypedExample(example);
        }

        // no matching example
        return empty();
    }

    private static <T> Optional<ContentTypedHolder<T>> convertMapEntryToContentTypedExample(Map.Entry<String, T> entry) {
        return of(new ContentTypedHolder<T>(entry.getKey(), entry.getValue()));
    }

    private boolean serveFromSchema(RoutingContext routingContext, OpenAPI spec, ContentTypedHolder<Schema<?>> schema) {
        try {
            final ContentTypedHolder<?> dynamicExamples = schemaService.collectExamples(spec, schema);
            responseTransmissionService.transmitExample(routingContext, dynamicExamples);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error serving example from schema", e);
            return false;
        }
    }
}
