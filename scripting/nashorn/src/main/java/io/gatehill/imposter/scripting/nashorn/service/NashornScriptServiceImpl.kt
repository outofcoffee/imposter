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
package io.gatehill.imposter.scripting.nashorn.service

import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.scripting.common.JavaScriptUtil.wrapScript
import io.gatehill.imposter.scripting.nashorn.shim.ConsoleShim
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.util.MetricsUtil.doIfMetricsEnabled
import io.micrometer.core.instrument.Gauge
import jdk.nashorn.api.scripting.NashornScriptEngine
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.script.CompiledScript
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * @author Pete Cornish
 */
@Suppress("DEPRECATION")
class NashornScriptServiceImpl @Inject constructor(
    scriptEngineManager: ScriptEngineManager
) : ScriptService {
    private val scriptEngine: NashornScriptEngine

    init {
        scriptEngine = scriptEngineManager.getEngineByName("nashorn") as NashornScriptEngine

        doIfMetricsEnabled(METRIC_SCRIPT_CACHE_ENTRIES) { registry ->
            Gauge.builder(METRIC_SCRIPT_CACHE_ENTRIES) { compiledScripts.size() }
                .description("The number of cached compiled scripts")
                .register(registry)
        }
    }

    /**
     * Holds compiled scripts, with maximum number of entries determined by the environment
     * variable [.ENV_SCRIPT_CACHE_ENTRIES].
     */
    private val compiledScripts = CacheBuilder.newBuilder()
        .maximumSize(getEnv(ENV_SCRIPT_CACHE_ENTRIES)?.toLong() ?: DEFAULT_SCRIPT_CACHE_ENTRIES)
        .build<Path, CompiledScript>()

    private val shouldPrecompile = getEnv(ENV_SCRIPT_PRECOMPILE)?.toBoolean() != false

    override fun initScript(scriptFile: Path) {
        if (shouldPrecompile) {
            LOGGER.debug("Precompiling script: $scriptFile")
            getCompiledScript(scriptFile)
        }
    }

    override fun executeScript(
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        val scriptFile = ScriptUtil.resolveScriptPath(pluginConfig, resourceConfig.responseConfig.scriptFile)
        LOGGER.trace("Executing script file: {}", scriptFile)

        return try {
            val compiledScript = getCompiledScript(scriptFile)
            val bindings = SimpleBindings(runtimeContext.asMap())

            // JS environment affordances
            bindings["console"] = ConsoleShim(bindings)
            compiledScript.eval(bindings) as ReadWriteResponseBehaviour

        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    @Throws(ExecutionException::class)
    private fun getCompiledScript(scriptFile: Path): CompiledScript {
        return compiledScripts.get(scriptFile) {
            try {
                LOGGER.trace("Compiling script file: {}", scriptFile)
                val compileStartMs = System.currentTimeMillis()
                val cs = scriptEngine.compile(wrapScript(scriptFile))
                LOGGER.debug("Script: {} compiled in {}ms", scriptFile, System.currentTimeMillis() - compileStartMs)
                return@get cs
            } catch (e: Exception) {
                throw RuntimeException("Failed to compile script: $scriptFile", e)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(NashornScriptServiceImpl::class.java)
        private const val ENV_SCRIPT_CACHE_ENTRIES = "IMPOSTER_SCRIPT_CACHE_ENTRIES"
        private const val ENV_SCRIPT_PRECOMPILE = "IMPOSTER_SCRIPT_PRECOMPILE"
        private const val DEFAULT_SCRIPT_CACHE_ENTRIES = 20L
        private const val METRIC_SCRIPT_CACHE_ENTRIES = "script.cache.entries"
    }
}