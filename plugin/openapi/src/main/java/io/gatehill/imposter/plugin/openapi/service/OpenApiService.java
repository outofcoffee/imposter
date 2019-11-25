package io.gatehill.imposter.plugin.openapi.service;

import io.swagger.models.Scheme;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface OpenApiService {
    default OpenAPI combineSpecifications(List<OpenAPI> specs, String basePath) {
        return combineSpecifications(specs, basePath, null, null);
    }

    OpenAPI combineSpecifications(List<OpenAPI> specs, String basePath, Scheme scheme, String title);
}
