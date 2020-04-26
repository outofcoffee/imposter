package io.gatehill.imposter.plugin.openapi.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import static java.util.Optional.ofNullable;

/**
 * Utilities for handling OpenAPI refs.
 * <p>
 * See: https://swagger.io/docs/specification/using-ref/
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RefUtil {
    private static final String REF_PREFIX_RESPONSES = "#/components/responses/";
    private static final String REF_PREFIX_SCHEMAS = "#/components/schemas/";

    private RefUtil() {
    }

    public static ApiResponse lookupResponseRef(OpenAPI spec, ApiResponse referrer) {
        if (referrer.get$ref().startsWith(REF_PREFIX_RESPONSES)) {
            final String responseName = referrer.get$ref().substring(REF_PREFIX_RESPONSES.length());
            return ofNullable(spec.getComponents())
                    .flatMap(components -> ofNullable(components.getResponses()))
                    .map(responses -> responses.get(responseName))
                    .orElseThrow(() -> new IllegalStateException("Referenced response not found in components section: " + responseName));
        } else {
            throw new IllegalStateException("Unsupported response $ref: " + referrer.get$ref());
        }
    }

    public static Schema<?> lookupSchemaRef(OpenAPI spec, Schema<?> referrer) {
        if (referrer.get$ref().startsWith(REF_PREFIX_SCHEMAS)) {
            final String schemaName = referrer.get$ref().substring(REF_PREFIX_SCHEMAS.length());
            return ofNullable(spec.getComponents())
                    .flatMap(components -> ofNullable(components.getSchemas()))
                    .map(responses -> responses.get(schemaName))
                    .orElseThrow(() -> new IllegalStateException("Referenced schema not found in components section: " + schemaName));
        } else {
            throw new IllegalStateException("Unsupported schema $ref: " + referrer.get$ref());
        }
    }
}
