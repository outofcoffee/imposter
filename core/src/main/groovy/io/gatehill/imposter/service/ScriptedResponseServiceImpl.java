/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks;
import io.gatehill.imposter.lifecycle.ScriptExecLifecycleHooks;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptUtil;
import io.gatehill.imposter.util.EnvVars;
import io.gatehill.imposter.util.LogUtil;
import io.gatehill.imposter.util.MetricsUtil;
import io.gatehill.imposter.util.annotation.GroovyImpl;
import io.gatehill.imposter.util.annotation.JavascriptImpl;
import io.micrometer.core.instrument.Timer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish
 */
public class ScriptedResponseServiceImpl implements ScriptedResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ScriptedResponseServiceImpl.class);
    private static final String METRIC_SCRIPT_EXECUTION_DURATION = "script.execution.duration";

    /**
     * Caches loggers to avoid logging framework lookup cost.
     */
    private final Cache<String, Logger> loggerCache = CacheBuilder.newBuilder().maximumSize(20).build();

    @Inject
    @GroovyImpl
    private ScriptService groovyScriptService;

    @Inject
    @JavascriptImpl
    private ScriptService javascriptScriptService;

    @Inject
    private EngineLifecycleHooks engineLifecycle;

    @Inject
    private ScriptExecLifecycleHooks scriptExecLifecycle;

    private Timer executionTimer;

    @Inject
    public ScriptedResponseServiceImpl() {
        MetricsUtil.doIfMetricsEnabled(METRIC_SCRIPT_EXECUTION_DURATION, registry -> {
            executionTimer = Timer
                    .builder(METRIC_SCRIPT_EXECUTION_DURATION)
                    .description("Script engine execution duration in seconds")
                    .register(registry);

        }).orElseDo(() -> {
            executionTimer = null;
        });
    }

    @Override
    public ReadWriteResponseBehaviour determineResponseFromScript(RoutingContext routingContext, PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, Map<String, ?> additionalContext, Map<String, ?> additionalBindings) {
        try {
            final Callable<ReadWriteResponseBehaviour> scriptExecutor = () -> determineResponseFromScriptInternal(
                    routingContext,
                    pluginConfig,
                    resourceConfig,
                    additionalContext,
                    additionalBindings
            );
            if (nonNull(executionTimer)) {
                return executionTimer.recordCallable(scriptExecutor);
            } else {
                return scriptExecutor.call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ReadWriteResponseBehaviour determineResponseFromScriptInternal(
            RoutingContext routingContext,
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            Map<String, ?> additionalContext,
            Map<String, ?> additionalBindings
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

            final Map<String, ?> finalAdditionalBindings = finaliseAdditionalBindings(routingContext, additionalBindings, executionContext);
            final Logger scriptLogger = buildScriptLogger(responseConfig);

            final RuntimeContext runtimeContext = new RuntimeContext(
                    EnvVars.getEnv(),
                    scriptLogger,
                    pluginConfig,
                    finalAdditionalBindings,
                    executionContext
            );

            // execute the script and read response behaviour
            final ReadWriteResponseBehaviour responseBehaviour =
                    fetchScriptService(responseConfig.getScriptFile()).executeScript(pluginConfig, resourceConfig, runtimeContext);

            // fire post execution hooks
            scriptExecLifecycle.forEach(listener -> listener.afterSuccessfulScriptExecution(finalAdditionalBindings, responseBehaviour));

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

    private Logger buildScriptLogger(ResponseConfig responseConfig) throws ExecutionException {
        final String scriptFile = responseConfig.getScriptFile();
        final String name;
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            name = scriptFile.substring(0, dotIndex);
        } else {
            name = scriptFile;
        }
        final String loggerName = LogUtil.LOGGER_SCRIPT_PACKAGE + "." + name;
        return loggerCache.get(loggerName, () -> LogManager.getLogger(loggerName));
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

    private Map<String, ?> finaliseAdditionalBindings(RoutingContext routingContext, Map<String, ?> additionalBindings, ExecutionContext executionContext) {
        Map<String, ?> finalAdditionalBindings = additionalBindings;

        // fire pre-context build hooks
        if (!engineLifecycle.isEmpty()) {
            final Map<String, Object> listenerAdditionalBindings = new HashMap<>();

            engineLifecycle.forEach(listener -> listener.beforeBuildingRuntimeContext(routingContext, listenerAdditionalBindings, executionContext));

            if (!listenerAdditionalBindings.isEmpty()) {
                listenerAdditionalBindings.putAll(additionalBindings);
                finalAdditionalBindings = listenerAdditionalBindings;
            }
        }
        return finalAdditionalBindings;
    }
}
