package io.gatehill.imposter.lifecycle;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;

/**
 * Hooks for engine lifecycle events.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ImposterLifecycleListener {
    /**
     * Invoked after inbuilt and plugin routes have been configured.
     *
     * @param imposterConfig   the Imposter configuration
     * @param allPluginConfigs all plugin configurations
     * @param router           the router
     */
    default void afterRoutesConfigured(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs, Router router) {
        // no op
    }

    /**
     * Invoked on each request to determine if the request is permitted to proceed.
     *
     * @param rootResourceConfig      the root resource configuration
     * @param resourceConfig          the resource configuration for this request
     * @param resolvedResourceConfigs the resolved resource configurations
     * @param routingContext          the routing context
     * @return {@code true} if the request is permitted, otherwise {@code false}
     */
    default boolean isRequestPermitted(
            ResponseConfigHolder rootResourceConfig,
            ResponseConfigHolder resourceConfig,
            List<ResolvedResourceConfig> resolvedResourceConfigs,
            RoutingContext routingContext
    ) {
        return true;
    }

    /**
     * Invoked before building the script runtime context.
     *
     * @param additionalBindings the additional bindings that will be passed to the script
     * @param executionContext   the script execution context
     */
    default void beforeBuildingRuntimeContext(Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        // no op
    }

    /**
     * Invoked following successful execution of the script.
     *
     * @param additionalBindings the additional bindings that were passed to the script
     * @param responseBehaviour  the result of the script execution
     */
    default void afterSuccessfulScriptExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour) {
        // no op
    }

    default String beforeTransmittingTemplate(RoutingContext routingContext, String responseTemplate) {
        // no op
        return responseTemplate;
    }
}
