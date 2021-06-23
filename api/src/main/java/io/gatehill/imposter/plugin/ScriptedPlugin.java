package io.gatehill.imposter.plugin;

import com.google.inject.Injector;
import io.gatehill.imposter.http.DefaultResponseBehaviourFactory;
import io.gatehill.imposter.http.DefaultStatusCodeFactory;
import io.gatehill.imposter.http.ResponseBehaviourFactory;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
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
    default void scriptHandler(
            C pluginConfig,
            RoutingContext routingContext,
            Injector injector,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                pluginConfig,
                routingContext,
                injector,
                null,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            ResponseConfigHolder resourceConfig,
            RoutingContext routingContext,
            Injector injector,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                resourceConfig,
                routingContext,
                injector,
                null,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            RoutingContext routingContext,
            Injector injector,
            Map<String, Object> additionalContext,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                pluginConfig,
                routingContext,
                injector,
                additionalContext,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            ResponseConfigHolder resourceConfig,
            RoutingContext routingContext,
            Injector injector,
            Map<String, Object> additionalContext,
            StatusCodeFactory statusCodeFactory,
            ResponseBehaviourFactory responseBehaviourFactory,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        final ResponseService responseService = injector.getInstance(ResponseService.class);
        responseService.handle(
                pluginConfig,
                resourceConfig,
                routingContext,
                injector,
                additionalContext,
                statusCodeFactory,
                responseBehaviourFactory,
                defaultBehaviourHandler
        );
    }
}
