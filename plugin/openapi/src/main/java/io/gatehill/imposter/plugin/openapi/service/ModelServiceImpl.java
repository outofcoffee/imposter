package io.gatehill.imposter.plugin.openapi.service;

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

/**
 * Collects examples from model definitions.
 *
 * @author benjvoigt
 */
public class ModelServiceImpl implements ModelService {
    private static final Logger LOGGER = LogManager.getLogger(ModelServiceImpl.class);
    private static final String REF_PREFIX_SCHEMAS = "#/components/schemas/";

    @Override
    public Object collectExample(OpenAPI spec, Schema schema) {
        final Object example = collectExampleInternal(spec, schema);
        LOGGER.debug("Collected example from schema: {}", example);
        return example;
    }

    private Object collectExampleInternal(OpenAPI spec, Schema schema) {
        final Object example;

        if (null != schema.getExample()) {
            example = schema.getExample();

        } else if (null != schema.get$ref()) {
            example = lookupSchemaRef(spec, schema);

        } else if (ObjectSchema.class.isAssignableFrom(schema.getClass())) {
            final ObjectSchema objectSchema = (ObjectSchema) schema;
            example = collectProperties(spec, objectSchema.getProperties());

        } else if (ArraySchema.class.isAssignableFrom(schema.getClass())) {
            final ArraySchema arraySchema = (ArraySchema) schema;
            final Schema items = arraySchema.getItems();
            final List<Object> list = new ArrayList<>();
            list.add(collectExampleInternal(spec, items));
            example = list;

        } else {
            example = getPropertyDefault(schema);
        }
        return example;
    }

    private Map<String, Object> lookupSchemaRef(OpenAPI spec, Schema schema) {
        if (schema.get$ref().startsWith(REF_PREFIX_SCHEMAS)) {
            final String schemaName = schema.get$ref().substring(REF_PREFIX_SCHEMAS.length());
            final Schema model = spec.getComponents().getSchemas().get(schemaName);
            return collectModel(spec, model);
        } else {
            throw new IllegalStateException("Unsupported $ref: " + schema.get$ref());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> collectModel(OpenAPI spec, Schema model) {
        final Map<String, Object> map = new HashMap<>();

        if (null != model.getProperties()) {
            return collectProperties(spec, model.getProperties());

        } else if (null != model.get$ref()) {
            return lookupSchemaRef(spec, model);

        } else if (ComposedSchema.class.isAssignableFrom(model.getClass())) {
            final ComposedSchema composedSchema = (ComposedSchema) model;
            if (null != composedSchema.getAllOf()) {
                final List<Schema> allOf = composedSchema.getAllOf();
                allOf.forEach(e -> map.putAll(collectModel(spec, e)));
            }
        }
        return map;
    }

    private Map<String, Object> collectProperties(OpenAPI spec, Map<String, Schema> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> collectExampleInternal(spec, e.getValue())));
    }

    private Object getPropertyDefault(Schema schema) {
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
                return null;
        }
    }
}
