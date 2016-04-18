package com.gatehill.imposter.plugin;

import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.model.ResponseBehaviourType;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.service.ResponseService;
import com.gatehill.imposter.util.InjectorUtil;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptedPlugin<C extends BaseConfig> {
    default void scriptHandler(C config, RoutingContext routingContext, Consumer<ResponseBehaviour> defaultBehaviourHandler) {
        scriptHandler(config, routingContext, null, defaultBehaviourHandler);
    }

    default void scriptHandler(C config, RoutingContext routingContext, Map<String, Object> bindings,
                               Consumer<ResponseBehaviour> defaultBehaviourHandler) {

        final ResponseService responseService = InjectorUtil.getInjector().getInstance(ResponseService.class);

        try {
            final ResponseBehaviour responseBehaviour = responseService.getResponseBehaviour(routingContext, config, bindings);

            if (ResponseBehaviourType.IMMEDIATE_RESPONSE == responseBehaviour.getBehaviourType()) {
                routingContext.response()
                        .setStatusCode(responseBehaviour.getStatusCode())
                        .end();

            } else {
                // default behaviour
                defaultBehaviourHandler.accept(responseBehaviour);
            }

        } catch (Exception e) {
            routingContext.fail(e);
        }
    }
}
