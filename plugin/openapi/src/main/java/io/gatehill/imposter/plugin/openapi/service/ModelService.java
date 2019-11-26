package io.gatehill.imposter.plugin.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Collects examples from model definitions.
 *
 * @author benjvoigt
 */
public interface ModelService {
    Object collectExample(OpenAPI spec, Schema schema);
}
