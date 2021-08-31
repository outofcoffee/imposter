package io.gatehill.imposter.service;

import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptUtil;
import io.gatehill.imposter.util.annotation.GroovyImpl;
import io.gatehill.imposter.util.annotation.JavascriptImpl;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ScriptedResponseServiceImpl implements ScriptedResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ScriptedResponseServiceImpl.class);

    @Inject
    @GroovyImpl
    private ScriptService groovyScriptService;

    @Inject
    @JavascriptImpl
    private ScriptService javascriptScriptService;

    @Inject
    private ImposterLifecycleHooks lifecycleHooks;

    @Override
    public ReadWriteResponseBehaviour determineResponseFromScript(
            RoutingContext routingContext,
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            Map<String, Object> additionalContext,
            Map<String, Object> additionalBindings
    ) {
        final ResponseConfig responseConfig = resourceConfig.getResponseConfig();

        try {
            final long executionStart = System.nanoTime();
            LOGGER.trace(
                    "Executing script '{}' for request: {} {}",
                    responseConfig.getScriptFile(),
                    routingContext.request().method(),
                    routingContext.request().absoluteURI()
            );

            final ExecutionContext executionContext = ScriptUtil.buildContext(routingContext, additionalContext);
            LOGGER.trace("Context for request: {}", () -> executionContext);

            final Map<String, Object> finalAdditionalBindings = finaliseAdditionalBindings(routingContext, additionalBindings, executionContext);

            final RuntimeContext runtimeContext = new RuntimeContext(
                    System.getenv(),
                    LogManager.getLogger(determineScriptName(responseConfig.getScriptFile())),
                    pluginConfig,
                    finalAdditionalBindings,
                    executionContext
            );

            // execute the script and read response behaviour
            final ReadWriteResponseBehaviour responseBehaviour =
                    fetchScriptService(responseConfig.getScriptFile()).executeScript(pluginConfig, resourceConfig, runtimeContext);

            // fire post execution hooks
            lifecycleHooks.forEach(listener -> listener.afterSuccessfulScriptExecution(finalAdditionalBindings, responseBehaviour));

            LOGGER.debug(String.format(
                    "Executed script '%s' for request: %s %s in %.2fms",
                    responseConfig.getScriptFile(),
                    routingContext.request().method(),
                    routingContext.request().absoluteURI(),
                    (System.nanoTime() - executionStart) / 1000000f
            ));
            return responseBehaviour;

        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Error executing script: '%s' for request: %s %s",
                    responseConfig.getScriptFile(),
                    routingContext.request().method(),
                    routingContext.request().absoluteURI()
            ), e);
        }
    }

    private String determineScriptName(String scriptFile) {
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            return scriptFile.substring(0, dotIndex);
        } else {
            return scriptFile;
        }
    }

    private ScriptService fetchScriptService(String scriptFile) {
        final String scriptExtension;
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            scriptExtension = scriptFile.substring(dotIndex + 1);
        } else {
            scriptExtension = "";
        }

        switch (scriptExtension.toLowerCase()) {
            case "groovy":
                return groovyScriptService;
            case "js":
                return javascriptScriptService;
            default:
                throw new RuntimeException("Unable to determine script engine from script file name: " + scriptFile);
        }
    }

    private Map<String, Object> finaliseAdditionalBindings(RoutingContext routingContext, Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        Map<String, Object> finalAdditionalBindings = additionalBindings;

        // fire pre-context build hooks
        if (!lifecycleHooks.isEmpty()) {
            final Map<String, Object> listenerAdditionalBindings = new HashMap<>();

            lifecycleHooks.forEach(listener -> listener.beforeBuildingRuntimeContext(routingContext, listenerAdditionalBindings, executionContext));

            if (!listenerAdditionalBindings.isEmpty()) {
                listenerAdditionalBindings.putAll(additionalBindings);
                finalAdditionalBindings = listenerAdditionalBindings;
            }
        }
        return finalAdditionalBindings;
    }
}
