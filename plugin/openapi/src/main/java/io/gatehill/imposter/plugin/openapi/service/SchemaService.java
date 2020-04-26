package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Collects examples from schema definitions.
 *
 * @author benjvoigt
 */
public interface SchemaService {
    ContentTypedHolder<?> collectExamples(OpenAPI spec, ContentTypedHolder<Schema<?>> schema);
}
