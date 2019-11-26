package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ExampleService {
    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param imposterConfig    the Imposter engine configuration
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     * @param spec              the OpenAPI specification
     * @return {@code true} if an example was served, otherwise {@code false}
     */
    boolean serveExample(ImposterConfig imposterConfig, OpenApiPluginConfig config,
                         RoutingContext routingContext,
                         ResponseBehaviour responseBehaviour,
                         ApiResponse mockResponse,
                         OpenAPI spec);
}
