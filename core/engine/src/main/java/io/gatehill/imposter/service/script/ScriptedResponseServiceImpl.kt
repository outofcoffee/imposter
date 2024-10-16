/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.service.script

import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.expression.helper.RandomHelper
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.ScriptLifecycleHooks
import io.gatehill.imposter.model.steps.ScriptProcessingStep
import io.gatehill.imposter.model.steps.ScriptStepContext
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.EvalResourceConfig
import io.gatehill.imposter.plugin.config.steps.StepType
import io.gatehill.imposter.script.ExecutionContext
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptSource
import io.gatehill.imposter.service.ScriptedResponseService
import io.gatehill.imposter.service.StepService
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.MetricsUtil
import io.micrometer.core.instrument.Timer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.util.Supplier
import java.util.concurrent.ExecutionException
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ScriptedResponseServiceImpl @Inject constructor(
    engineLifecycle: EngineLifecycleHooks,
    private val scriptLifecycle: ScriptLifecycleHooks,
    private val scriptServiceFactory: ScriptServiceFactory,
    private val evalScriptService: EvalScriptService,
    private val stepService: StepService,
) : ScriptedResponseService, EngineLifecycleListener {

    /**
     * Caches loggers to avoid logging framework lookup cost.
     */
    private val loggerCache = CacheBuilder.newBuilder().maximumSize(20).build<String, Logger>()

    private var executionTimer: Timer? = null

    init {
        MetricsUtil.doIfMetricsEnabled(METRIC_SCRIPT_EXECUTION_DURATION) { registry ->
            executionTimer = Timer
                .builder(METRIC_SCRIPT_EXECUTION_DURATION)
                .description("Script engine execution duration in seconds")
                .register(registry)
        }.orElseDo { executionTimer = null }

        engineLifecycle.registerListener(this)
    }

    override fun afterRoutesConfigured(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        router: HttpRouter
    ) {
        initScripts(allPluginConfigs)
    }

    private fun initScripts(allPluginConfigs: List<PluginConfig>) {
        // root resource
        allPluginConfigs.forEach { config ->
            if (config is BasicResourceConfig) {
                initScriptFilesAndSteps(config, config)
            }
        }

        // child resources
        allPluginConfigs.forEach { config ->
            if (config is ResourcesHolder<*>) {
                config.resources?.forEach { resource ->
                    initScriptFilesAndSteps(config, resource)

                    // inline eval scripts
                    if (resource is EvalResourceConfig) {
                        evalScriptService.initScript(resource)
                    }
                }
            }
        }
    }

    /**
     * Covers `scriptFile` on response config and script steps.
     */
    private fun initScriptFilesAndSteps(config: PluginConfig, resource: BasicResourceConfig) {
        val steps = stepService.determineSteps(config, resource)
        steps.filter { it.type == StepType.Script }.forEach { step ->
            val context = step.context as ScriptStepContext
            val script = ScriptProcessingStep.parseScriptSource(context)

            val scriptService = scriptServiceFactory.fetchScriptService(script.source)
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Initialising script: {} using: {}", script.source, scriptService::class.qualifiedName)
            }
            scriptService.initScript(script)
        }
    }

    override fun determineResponseFromScript(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        script: ScriptSource,
        additionalContext: Map<String, Any>?
    ): ReadWriteResponseBehaviour {
        return try {
            val scriptExecutor = {
                execute(
                    httpExchange,
                    pluginConfig,
                    script,
                    additionalContext
                )
            }
            executionTimer?.recordCallable(scriptExecutor) ?: scriptExecutor()

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun execute(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        script: ScriptSource,
        additionalContext: Map<String, Any>?
    ): ReadWriteResponseBehaviour {
        try {
            val executionStart = System.nanoTime()
            LOGGER.trace(
                "Executing script '{}' for request: {}",
                script,
                LogUtil.describeRequestShort(httpExchange)
            )

            // execute the script using an appropriate implementation and read response behaviour
            val scriptService = scriptServiceFactory.fetchScriptService(script.source)

            val executionContext = ScriptUtil.buildContext(scriptService.requestBuilder, httpExchange, additionalContext)
            LOGGER.trace("Context for request: {}", Supplier<Any> { executionContext })

            val additionalBindings = getAdditionalBindings(httpExchange, scriptService.implName, executionContext)
            val scriptLogger = buildScriptLogger(script.source)

            val runtimeContext = RuntimeContext(
                EnvVars.getEnv(),
                scriptLogger,
                pluginConfig,
                additionalBindings,
                executionContext
            )

            val responseBehaviour = scriptService.executeScript(script, runtimeContext)

            // fire post execution hooks
            scriptLifecycle.forEach { listener ->
                listener.afterSuccessfulScriptExecution(additionalBindings, responseBehaviour)
            }

            val scriptDuration = (System.nanoTime() - executionStart) / 1000000f

            // used for summary logging
            httpExchange.put(LogUtil.KEY_SCRIPT_DURATION, scriptDuration)

            LOGGER.debug(
                String.format(
                    "Executed script '%s' for request: %s in %.2fms",
                    script,
                    LogUtil.describeRequestShort(httpExchange),
                    scriptDuration
                )
            )
            return responseBehaviour

        } catch (e: Exception) {
            throw RuntimeException(
                "Error executing script: '$script' for request: " +
                        LogUtil.describeRequestShort(httpExchange), e
            )
        }
    }

    @Throws(ExecutionException::class)
    private fun buildScriptLogger(scriptSource: String): Logger {
        val name: String?
        val dotIndex = scriptSource.lastIndexOf('.')
        name = if (dotIndex >= 1 && dotIndex < scriptSource.length - 1) {
            scriptSource.substring(0, dotIndex)
        } else {
            scriptSource
        }
        val loggerName = LogUtil.LOGGER_SCRIPT_PACKAGE + "." + name
        return loggerCache[loggerName, { LogManager.getLogger(loggerName) }]
    }

    private fun getAdditionalBindings(
        httpExchange: HttpExchange,
        scriptEngineName: String,
        executionContext: ExecutionContext
    ): Map<String, Any> {
        val additionalBindings = mutableMapOf<String, Any>(
            "random" to RandomHelper
        )

        // fire pre-context build hooks
        if (!scriptLifecycle.isEmpty) {
            scriptLifecycle.forEach { listener ->
                listener.beforeBuildingRuntimeContext(
                    httpExchange,
                    scriptEngineName,
                    additionalBindings,
                    executionContext
                )
            }
        }
        return additionalBindings
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ScriptedResponseServiceImpl::class.java)
        private const val METRIC_SCRIPT_EXECUTION_DURATION = "script.execution.duration"
    }
}
