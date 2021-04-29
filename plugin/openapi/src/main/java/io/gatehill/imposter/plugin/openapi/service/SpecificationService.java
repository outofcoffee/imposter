package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.swagger.models.Scheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface SpecificationService {
    default OpenAPI combineSpecifications(List<OpenAPI> specs, String basePath) {
        return combineSpecifications(specs, basePath, null, null);
    }

    /**
     * Returns the combined specification from cache, generating it first on cache miss.
     */
    OpenAPI getCombinedSpec(ImposterConfig imposterConfig, List<OpenAPI> allSpecs) throws ExecutionException;

    /**
     * As {@link #getCombinedSpec(ImposterConfig, List)} but serialised to JSON.
     */
    String getCombinedSpecSerialised(ImposterConfig imposterConfig, List<OpenAPI> allSpecs) throws ExecutionException;

    OpenAPI combineSpecifications(List<OpenAPI> specs, String basePath, Scheme scheme, String title);

    boolean isValidRequest(ImposterConfig imposterConfig,
                           OpenApiPluginConfig pluginConfig,
                           RoutingContext routingContext,
                           List<OpenAPI> allSpecs);
}
