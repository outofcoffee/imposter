package io.gatehill.imposter.service;

import com.google.inject.Injector;
import io.gatehill.imposter.http.ResponseBehaviourFactory;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseService {
    void registerListener(ScriptedResponseListener listener);

    JsonArray loadResponseAsJsonArray(PluginConfig config, ResponseBehaviour behaviour);

    JsonArray loadResponseAsJsonArray(PluginConfig config, String responseFile);

    void handle(
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            RoutingContext routingContext,
            Injector injector,
            Map<String, Object> additionalContext,
            StatusCodeFactory statusCodeFactory,
            ResponseBehaviourFactory responseBehaviourFactory,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    );

    /**
     * Send an empty response to the client, typically used as a fallback when no
     * other response can be computed.
     *
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @return always {@code true}
     */
    boolean sendEmptyResponse(RoutingContext routingContext, ResponseBehaviour responseBehaviour);

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, an empty response is returned.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    void sendResponse(
            PluginConfig pluginConfig,
            ResourceConfig resourceConfig,
            RoutingContext routingContext,
            ResponseBehaviour responseBehaviour
    );

    /**
     * Send a response to the client, if one can be computed. If a response cannot
     * be computed, each of the fallbackSenders is invoked until a response is sent.
     *
     * @param pluginConfig      the plugin configuration
     * @param resourceConfig    the resource configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     * @param fallbackSenders   the handler(s) to invoke in sequence if a response cannot be computed
     */
    void sendResponse(
            PluginConfig pluginConfig,
            ResourceConfig resourceConfig,
            RoutingContext routingContext,
            ResponseBehaviour responseBehaviour,
            ResponseSender... fallbackSenders
    );

    @FunctionalInterface
    interface ResponseSender {
        boolean send(RoutingContext routingContext, ResponseBehaviour responseBehaviour) throws Exception;
    }

    /**
     * Hooks for script lifecycle events.
     */
    interface ScriptedResponseListener {
        /**
         * Invoked before building the runtime context.
         *
         * @param additionalBindings the additional bindings that will be passed to the script
         * @param executionContext   the script execution context
         */
        void beforeBuildingRuntimeContext(Map<String, Object> additionalBindings, ExecutionContext executionContext);

        /**
         * Invoked following successful execution of the script.
         *
         * @param additionalBindings the additional bindings that were passed to the script
         * @param responseBehaviour  the result of the script execution
         */
        void afterSuccessfulExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour);
    }
}
