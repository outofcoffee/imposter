package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.gatehill.imposter.plugin.openapi.util.RefUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Collects examples from schema definitions.
 *
 * @author benjvoigt
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SchemaServiceImpl implements SchemaService {
    private static final Logger LOGGER = LogManager.getLogger(SchemaServiceImpl.class);

    @Override
    public ContentTypedHolder<?> collectExamples(OpenAPI spec, ContentTypedHolder<Schema<?>> schema) {
        final Object example = collectSchemaExample(spec, schema.getValue());
        LOGGER.debug("Collected example from schema: {}", example);
        return new ContentTypedHolder<>(schema.getContentType(), example);
    }

    private Object collectSchemaExample(OpenAPI spec, Schema<?> schema) {
        final Object example;

        // $ref takes precedence, per spec:
        //   "Any sibling elements of a $ref are ignored. This is because
        //   $ref works by replacing itself and everything on its level
        //   with the definition it is pointing at."
        // See: https://swagger.io/docs/specification/using-ref/
        if (nonNull(schema.get$ref())) {
            final Schema<?> referent = RefUtil.lookupSchemaRef(spec, schema);
            example = collectSchemaExample(spec, referent);

        } else if (nonNull(schema.getExample())) {
            example = schema.getExample();

        } else if (nonNull(schema.getProperties())) {
            example = buildFromProperties(spec, schema.getProperties());

        } else if (ObjectSchema.class.isAssignableFrom(schema.getClass())) {
            final ObjectSchema objectSchema = (ObjectSchema) schema;
            example = buildFromProperties(spec, objectSchema.getProperties());

        } else if (ArraySchema.class.isAssignableFrom(schema.getClass())) {
            example = buildFromArraySchema(spec, (ArraySchema) schema);

        } else if (ComposedSchema.class.isAssignableFrom(schema.getClass())) {
            example = buildFromComposedSchema(spec, (ComposedSchema) schema);

        } else {
            example = getPropertyDefault(schema);
        }

        return example;
    }

    private List<Object> buildFromArraySchema(OpenAPI spec, ArraySchema schema) {
        // items may be a schema type with multiple children
        final Schema items = schema.getItems();
        final List<Object> examples = new ArrayList<>();
        examples.add(collectSchemaExample(spec, items));
        return examples;
    }

    private Object buildFromComposedSchema(OpenAPI spec, ComposedSchema schema) {
        final Object example;
        if (nonNull(schema.getAllOf()) && !schema.getAllOf().isEmpty()) {
            final List<Schema> allOf = schema.getAllOf();

            // Combine properties of 'allOf'
            // See: https://swagger.io/docs/specification/data-models/oneof-anyof-allof-not/
            final Map<String, Object> combinedExampleProperties = new HashMap<>();
            allOf.forEach(s -> {
                final Object exampleMap = collectSchemaExample(spec, s);
                if (nonNull(exampleMap) && exampleMap instanceof Map) {
                    // FIXME code defensively around this cast
                    combinedExampleProperties.putAll((Map<String, Object>) exampleMap);
                }
            });
            example = combinedExampleProperties;

        } else if (nonNull(schema.getOneOf()) && !schema.getOneOf().isEmpty()) {
            LOGGER.debug("Found 'oneOf' in schema {} - using first schema example", schema.getName());
            final List<Schema> oneOf = schema.getOneOf();
            example = collectSchemaExample(spec, oneOf.get(0));

        } else if (nonNull(schema.getAnyOf()) && !schema.getAnyOf().isEmpty()) {
            LOGGER.debug("Found 'anyOf' in schema {} - using first schema example", schema.getName());
            final List<Schema> anyOf = schema.getAnyOf();
            example = collectSchemaExample(spec, anyOf.get(0));

        } else if (nonNull(schema.getNot())) {
            LOGGER.debug("Found 'not' in schema {} - using null for schema example", schema.getName());
            example = null;

        } else {
            throw new IllegalStateException("Invalid composed schema; missing or empty [allOf, oneOf, anyOf]");
        }
        return example;
    }

    private Map<String, Object> buildFromProperties(OpenAPI spec, Map<String, Schema> properties) {
        if (isNull(properties)) {
            return emptyMap();
        }
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> collectSchemaExample(spec, e.getValue())));
    }

    private Object getPropertyDefault(Schema schema) {
        if (nonNull(schema.getType())) {
            // TODO make these configurable
            switch (schema.getType()) {
                case "string":
                    return "";
                case "number":
                case "integer":
                    return 0;
                case "boolean":
                    return false;
                default:
                    LOGGER.warn("Unknown type: {} for schema: {} - returning null for example property", schema.getType(), schema.getName());
                    return null;
            }
        }
        LOGGER.warn("Missing type for schema: {} - returning null for example property", schema.getName());
        return null;
    }
}
