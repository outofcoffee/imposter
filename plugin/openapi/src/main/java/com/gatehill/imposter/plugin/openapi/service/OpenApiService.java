package com.gatehill.imposter.plugin.openapi.service;

import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface OpenApiService {
    Swagger combineSpecifications(List<Swagger> specs, String basePath);

    Swagger combineSpecifications(List<Swagger> specs, String basePath, Scheme scheme, String title);
}
