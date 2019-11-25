package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.function.BiConsumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ExampleService {
    /**
     * Attempt to respond with an example from the API specification.
     *
     * @param config            the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param mockResponse      the specification response
     * @param fallback          callback to invoke if no example was served
     */
    void serveExample(OpenApiPluginConfig config,
                      RoutingContext routingContext,
                      ResponseBehaviour responseBehaviour,
                      ApiResponse mockResponse,
                      BiConsumer<RoutingContext, ResponseBehaviour> fallback);
}
