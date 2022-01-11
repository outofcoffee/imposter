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
package io.gatehill.imposter.service.script

import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.ScriptExecLifecycleHooks
import io.gatehill.imposter.lifecycle.ScriptExecutionLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.ResponseConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ExecutionContext
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptedResponseService
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
    private val engineLifecycle: EngineLifecycleHooks,
    private val scriptExecLifecycle: ScriptExecLifecycleHooks,
    private val scriptServiceFactory: ScriptServiceFactory
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

    override fun afterRoutesConfigured(imposterConfig: ImposterConfig, allPluginConfigs: List<PluginConfig>, router: HttpRouter) {
        initScripts(allPluginConfigs)
    }

    private fun initScripts(allPluginConfigs: List<PluginConfig>) {
        val allScripts = mutableListOf<Pair<PluginConfig, ResponseConfig>>()

        // root resource
        allPluginConfigs.filter { it is ResponseConfigHolder }.forEach { config ->
            allScripts += config to (config as ResponseConfigHolder).responseConfig
        }
        // child resources
        allPluginConfigs.filter { it is ResourcesHolder<*> }.forEach { config ->
            (config as ResourcesHolder<*>).resources?.forEach { resource ->
                allScripts += config to resource.responseConfig
            }
        }

        allScripts.distinctBy { (_, responseConfig) -> responseConfig.scriptFile }
            .forEach { (config, responseConfig) -> initScript(config, responseConfig) }
    }

    fun initScript(pluginConfig: PluginConfig, responseConfig: ResponseConfig) {
        responseConfig.scriptFile?.let { scriptFile ->
            val scriptPath = ScriptUtil.resolveScriptPath(pluginConfig, scriptFile)
            scriptServiceFactory.fetchScriptService(scriptFile).initScript(scriptPath)
        }
    }

    override fun determineResponseFromScript(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder?,
        additionalContext: Map<String, Any>?,
        additionalBindings: Map<String, Any>?
    ): ReadWriteResponseBehaviour {
        return try {
            val scriptExecutor = {
                determineResponseFromScriptInternal(
                    httpExchange,
                    pluginConfig,
                    resourceConfig,
                    additionalContext,
                    additionalBindings
                )
            }
            executionTimer?.recordCallable(scriptExecutor) ?: scriptExecutor()

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun determineResponseFromScriptInternal(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder?,
        additionalContext: Map<String, Any>?,
        additionalBindings: Map<String, Any>?
    ): ReadWriteResponseBehaviour {
        val responseConfig = resourceConfig!!.responseConfig

        checkNotNull(responseConfig.scriptFile) { "Script file not set" }
        val scriptFile = responseConfig.scriptFile!!

        try {
            val executionStart = System.nanoTime()
            LOGGER.trace(
                "Executing script '{}' for request: {} {}",
                responseConfig.scriptFile,
                httpExchange.request().method(),
                httpExchange.request().absoluteURI()
            )
            val executionContext = ScriptUtil.buildContext(httpExchange, additionalContext)
            LOGGER.trace("Context for request: {}", Supplier<Any> { executionContext })

            val finalAdditionalBindings = finaliseAdditionalBindings(
                httpExchange,
                additionalBindings ?: emptyMap(),
                executionContext
            )

            val scriptLogger = buildScriptLogger(responseConfig)
            val runtimeContext = RuntimeContext(
                EnvVars.getEnv(),
                scriptLogger,
                pluginConfig,
                finalAdditionalBindings,
                executionContext
            )

            // execute the script and read response behaviour
            val responseBehaviour = scriptServiceFactory.fetchScriptService(scriptFile).executeScript(
                pluginConfig,
                resourceConfig,
                runtimeContext
            )

            // fire post execution hooks
            scriptExecLifecycle.forEach { listener: ScriptExecutionLifecycleListener ->
                listener.afterSuccessfulScriptExecution(finalAdditionalBindings, responseBehaviour)
            }

            val scriptDuration = (System.nanoTime() - executionStart) / 1000000f

            // used for summary logging
            httpExchange.put(LogUtil.KEY_SCRIPT_DURATION, scriptDuration)

            LOGGER.debug(
                String.format(
                    "Executed script '%s' for request: %s %s in %.2fms",
                    responseConfig.scriptFile,
                    httpExchange.request().method(),
                    httpExchange.request().absoluteURI(),
                    scriptDuration
                )
            )
            return responseBehaviour

        } catch (e: Exception) {
            throw RuntimeException(
                "Error executing script: '${responseConfig.scriptFile}' for request: " +
                    "${httpExchange.request().method()} ${httpExchange.request().absoluteURI()}", e
            )
        }
    }

    @Throws(ExecutionException::class)
    private fun buildScriptLogger(responseConfig: ResponseConfig): Logger {
        val scriptFile = responseConfig.scriptFile
        val name: String?
        val dotIndex = scriptFile!!.lastIndexOf('.')
        name = if (dotIndex >= 1 && dotIndex < scriptFile.length - 1) {
            scriptFile.substring(0, dotIndex)
        } else {
            scriptFile
        }
        val loggerName = LogUtil.LOGGER_SCRIPT_PACKAGE + "." + name
        return loggerCache[loggerName, { LogManager.getLogger(loggerName) }]
    }

    private fun finaliseAdditionalBindings(
        httpExchange: HttpExchange,
        additionalBindings: Map<String, Any>,
        executionContext: ExecutionContext
    ): Map<String, Any> {
        var finalAdditionalBindings = additionalBindings

        // fire pre-context build hooks
        if (!engineLifecycle.isEmpty) {
            val listenerAdditionalBindings: MutableMap<String, Any> = mutableMapOf()
            engineLifecycle.forEach { listener: EngineLifecycleListener ->
                listener.beforeBuildingRuntimeContext(
                    httpExchange, listenerAdditionalBindings, executionContext
                )
            }
            if (listenerAdditionalBindings.isNotEmpty()) {
                listenerAdditionalBindings.putAll(additionalBindings)
                finalAdditionalBindings = listenerAdditionalBindings
            }
        }
        return finalAdditionalBindings
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ScriptedResponseServiceImpl::class.java)
        private const val METRIC_SCRIPT_EXECUTION_DURATION = "script.execution.duration"
    }
}
