package com.gatehill.imposter.service;

import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.config.ResourceConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseService {
    JsonArray loadResponseAsJsonArray(ResponseBehaviour behaviour);

    JsonArray loadResponseAsJsonArray(String responseFile);

    ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, ResourceConfig config,
                                           Map<String, Object> additionalContext, Map<String, Object> bindings);
}
