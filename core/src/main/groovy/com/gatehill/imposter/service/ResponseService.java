package com.gatehill.imposter.service;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.config.BaseConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseService {
    JsonArray loadResponseAsJsonArray(ImposterConfig imposterConfig, ResponseBehaviour behaviour);

    ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, BaseConfig config,
                                           Map<String, Object> additionalContext, Map<String, Object> bindings);
}
