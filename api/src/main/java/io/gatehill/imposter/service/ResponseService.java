package io.gatehill.imposter.service;

import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.http.ResponseBehaviourFactory;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseService {
    JsonArray loadResponseAsJsonArray(PluginConfig config, ResponseBehaviour behaviour);

    JsonArray loadResponseAsJsonArray(PluginConfig config, String responseFile);

    ResponseBehaviour buildResponseBehaviour(RoutingContext routingContext,
                                             PluginConfig pluginConfig,
                                             ResponseConfigHolder config,
                                             Map<String, Object> additionalContext,
                                             Map<String, Object> additionalBindings,
                                             StatusCodeFactory statusCodeFactory,
                                             ResponseBehaviourFactory responseBehaviourFactory);

    List<ResolvedResourceConfig> resolveResourceConfigs(ResponseConfigHolder config);

    /**
     * Search for a resource configuration matching the current request.
     *
     * @param resources    the resources from the response configuration
     * @param method       the HTTP method of the current request
     * @param pathTemplate request path template
     * @param path         the path of the current request
     * @param pathParams   the path parameters of the current request
     * @param queryParams  the query parameters of the current request
     * @return a matching resource configuration or else empty
     */
    Optional<ResponseConfigHolder> matchResourceConfig(List<ResolvedResourceConfig> resources,
                                                       HttpMethod method,
                                                       String pathTemplate,
                                                       String path,
                                                       Map<String, String> pathParams,
                                                       Map<String, String> queryParams);

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
    void sendResponse(PluginConfig pluginConfig,
                      ContentTypedConfig resourceConfig,
                      RoutingContext routingContext,
                      ResponseBehaviour responseBehaviour);

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
    void sendResponse(PluginConfig pluginConfig,
                      ContentTypedConfig resourceConfig,
                      RoutingContext routingContext,
                      ResponseBehaviour responseBehaviour,
                      ResponseSender... fallbackSenders);

    @FunctionalInterface
    interface ResponseSender {
        boolean send(RoutingContext routingContext, ResponseBehaviour responseBehaviour) throws Exception;
    }
}
