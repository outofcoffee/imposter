package io.gatehill.imposter.plugin;

import com.google.inject.Injector;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.service.ResponseService;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptedPlugin<C extends PluginConfigImpl> {
    default void scriptHandler(C pluginConfig,
                               RoutingContext routingContext,
                               Injector injector,
                               Consumer<ResponseBehaviour> defaultBehaviourHandler) {

        scriptHandler(pluginConfig, pluginConfig, routingContext, injector, null, defaultBehaviourHandler);
    }

    default void scriptHandler(C pluginConfig,
                               ResourceConfig resourceConfig,
                               RoutingContext routingContext,
                               Injector injector,
                               Consumer<ResponseBehaviour> defaultBehaviourHandler) {

        scriptHandler(pluginConfig, resourceConfig, routingContext, injector, null, defaultBehaviourHandler);
    }

    default void scriptHandler(C pluginConfig,
                               RoutingContext routingContext,
                               Injector injector,
                               Map<String, Object> additionalContext,
                               Consumer<ResponseBehaviour> defaultBehaviourHandler) {

        scriptHandler(pluginConfig, pluginConfig, routingContext, injector, additionalContext, defaultBehaviourHandler);
    }

    default void scriptHandler(C pluginConfig,
                               ResourceConfig resourceConfig,
                               RoutingContext routingContext,
                               Injector injector,
                               Map<String, Object> additionalContext,
                               Consumer<ResponseBehaviour> defaultBehaviourHandler) {

        final ResponseService responseService = injector.getInstance(ResponseService.class);

        try {
            final ResponseBehaviour responseBehaviour = responseService.getResponseBehaviour(
                    routingContext, pluginConfig, resourceConfig, additionalContext, Collections.emptyMap());

            if (ResponseBehaviourType.IMMEDIATE_RESPONSE.equals(responseBehaviour.getBehaviourType())) {
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
