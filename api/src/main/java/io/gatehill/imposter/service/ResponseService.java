package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseService {
    JsonArray loadResponseAsJsonArray(PluginConfig config, ResponseBehaviour behaviour);

    JsonArray loadResponseAsJsonArray(PluginConfig config, String responseFile);

    ResponseBehaviour getResponseBehaviour(RoutingContext routingContext,
                                           PluginConfig pluginConfig,
                                           ResourceConfig config,
                                           Map<String, Object> additionalContext,
                                           Map<String, Object> additionalBindings);

    /**
     * Send an empty response to the client, typically used as a fallback when no
     * other response can be computed.
     *
     * @param routingContext the Vert.x routing context
     */
    void sendEmptyResponse(RoutingContext routingContext);

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, an empty response is returned.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    void sendResponse(PluginConfig pluginConfig,
                      ContentTypedConfig resourceConfig,
                      RoutingContext routingContext,
                      ResponseBehaviour responseBehaviour);

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, the missingResponseSender is invoked.
     *
     * @param pluginConfig          the plugin configuration
     * @param resourceConfig        the resource configuration
     * @param routingContext        the Vert.x routing context
     * @param responseBehaviour     the response behaviour
     * @param missingResponseSender the handler to invoke if a response cannot be computed
     */
    void sendResponse(PluginConfig pluginConfig,
                      ContentTypedConfig resourceConfig,
                      RoutingContext routingContext,
                      ResponseBehaviour responseBehaviour,
                      ResponseSender missingResponseSender);

    /**
     * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
     */
    @FunctionalInterface
    interface ResponseSender {
        void send(RoutingContext routingContext) throws Exception;
    }
}
