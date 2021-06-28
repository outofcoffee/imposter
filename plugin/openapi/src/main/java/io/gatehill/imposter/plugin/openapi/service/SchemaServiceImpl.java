package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.gatehill.imposter.plugin.openapi.util.RefUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.vertx.core.http.HttpServerRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.from(ZoneOffset.UTC));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));

    private static final Map<String, DefaultValueProvider<?>> DEFAULT_VALUE_PROVIDERS = new HashMap<String, DefaultValueProvider<?>>() {{
        put("string", new StringDefaultValueProvider());
        put("number", new NumberDefaultValueProvider());
        put("integer", new IntegerDefaultValueProvider());
        put("boolean", new BooleanDefaultValueProvider());
    }};

    @Override
    public ContentTypedHolder<?> collectExamples(HttpServerRequest request, OpenAPI spec, ContentTypedHolder<Schema<?>> schema) {
        final Object example = collectSchemaExample(spec, schema.getValue());
        LOGGER.trace("Collected example from {} schema for {} {}: {}", schema.getContentType(), request.method(), request.absoluteURI(), example);

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
            if (schema instanceof DateTimeSchema) {
                example = DATE_TIME_FORMATTER.format((OffsetDateTime) schema.getExample());
            } else if (schema instanceof DateSchema) {
                example = DATE_FORMATTER.format(((Date) schema.getExample()).toInstant());
            } else {
                example = schema.getExample();
            }

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
        final Schema<?> items = schema.getItems();
        final List<Object> examples = new ArrayList<>();
        examples.add(collectSchemaExample(spec, items));
        return examples;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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

    @SuppressWarnings("rawtypes")
    private Map<String, Object> buildFromProperties(OpenAPI spec, Map<String, Schema> properties) {
        if (isNull(properties)) {
            return emptyMap();
        }
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> collectSchemaExample(spec, e.getValue())));
    }

    private Object getPropertyDefault(Schema<?> schema) {
        // if a non-empty enum exists, choose the first value
        if (nonNull(schema.getEnum()) && !schema.getEnum().isEmpty()) {
            return schema.getEnum().get(0);
        }

        // fall back to a default for the type
        if (nonNull(schema.getType())) {
            final DefaultValueProvider<?> defaultValueProvider = DEFAULT_VALUE_PROVIDERS.get(schema.getType());
            if (nonNull(defaultValueProvider)) {
                return defaultValueProvider.provide(schema);
            } else {
                LOGGER.warn("Unknown type: {} for schema: {} - returning null for example property", schema.getType(), schema.getName());
                return null;
            }
        }

        LOGGER.warn("Missing type for schema: {} - returning null for example property", schema.getName());
        return null;
    }

    private interface DefaultValueProvider<T> {
        T provide(Schema<?> schema);
    }

    private static class StringDefaultValueProvider implements DefaultValueProvider<String> {
        @Override
        public String provide(Schema<?> schema) {
            // TODO make these configurable
            if (nonNull(schema.getFormat())) {
                // see https://swagger.io/docs/specification/data-models/data-types/
                switch (schema.getFormat()) {
                    case "date":
                        return DATE_FORMATTER.format(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                    case "date-time":
                        return DATE_TIME_FORMATTER.format(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                    case "password":
                        return "changeme";
                    case "byte":
                        // base64-encoded characters
                        return "SW1wb3N0ZXI0bGlmZQo=";
                    case "email":
                        return "test@example.com";
                    case "uuid":
                    case "guid":
                        return UUID.randomUUID().toString();
                }
            }
            return "";
        }
    }

    private static class NumberDefaultValueProvider implements DefaultValueProvider<Double> {
        @Override
        public Double provide(Schema<?> schema) {
            return 0.0;
        }
    }

    private static class IntegerDefaultValueProvider implements DefaultValueProvider<Integer> {
        @Override
        public Integer provide(Schema<?> schema) {
            return 0;
        }
    }

    private static class BooleanDefaultValueProvider implements DefaultValueProvider<Boolean> {
        @Override
        public Boolean provide(Schema<?> schema) {
            return false;
        }
    }
}
