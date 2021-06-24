package io.gatehill.imposter.lifecycle;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.vertx.ext.web.Router;

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
     * @param imposterConfig
     * @param allPluginConfigs
     * @param router the router
     */
    void afterRoutesConfigured(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs, Router router);

    /**
     * Invoked before building the script runtime context.
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
    void afterSuccessfulScriptExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour);
}
