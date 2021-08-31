package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptedResponseService {
    ReadWriteResponseBehaviour determineResponseFromScript(
            RoutingContext routingContext,
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            Map<String, Object> additionalContext,
            Map<String, Object> additionalBindings
    );
}
