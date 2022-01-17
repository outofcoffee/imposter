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
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.scripting.common.util.CompiledJsScript
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.scripting.nashorn.NashornStandaloneScriptingModule
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.MetricsUtil.doIfMetricsEnabled
import io.gatehill.imposter.util.getJvmVersion
import io.micrometer.core.instrument.Gauge
import org.openjdk.nashorn.api.scripting.NashornScriptEngine
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import javax.script.CompiledScript
import javax.script.ScriptException
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * Standalone Nashorn implementation of JavaScript scripting engine
 * for JDK 11-14.
 *
 * @author Pete Cornish
 */
@Suppress("DEPRECATION")
@PluginInfo("js-nashorn-standalone")
@RequireModules(NashornStandaloneScriptingModule::class)
class NashornStandaloneScriptServiceImpl : ScriptService, Plugin {
    private val scriptEngine: NashornScriptEngine

    /**
     * Holds compiled scripts, with maximum number of entries determined by the environment
     * variable [ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES].
     */
    private val compiledScripts = CacheBuilder.newBuilder()
        .maximumSize(getEnv(ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES)?.toLong() ?: ScriptUtil.DEFAULT_SCRIPT_CACHE_ENTRIES)
        .build<Path, CompiledJsScript<CompiledScript>>()

    init {
        if (getJvmVersion() < 11) {
            throw UnsupportedOperationException("Standalone Nashorn JavaScript plugin is only supported on Java 11+. Use js-nashorn-embedded plugin instead.")
        }

        scriptEngine = NashornScriptEngineFactory().scriptEngine as NashornScriptEngine

        doIfMetricsEnabled(METRIC_SCRIPT_JS_CACHE_ENTRIES) { registry ->
            Gauge.builder(METRIC_SCRIPT_JS_CACHE_ENTRIES) { compiledScripts.size() }
                .description("The number of cached compiled JavaScript scripts")
                .register(registry)
        }
    }

    override fun initScript(scriptFile: Path) {
        if (ScriptUtil.shouldPrecompile) {
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
            val bindings = SimpleBindings(JavaScriptUtil.transformRuntimeMap(runtimeContext, true))

            val compiled = getCompiledScript(scriptFile)
            try {
                compiled.code.eval(bindings) as ReadWriteResponseBehaviour
            } catch (e: ScriptException) {
                throw JavaScriptUtil.unwrapScriptException(e, compiled)
            }

        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    private fun getCompiledScript(scriptFile: Path): CompiledJsScript<CompiledScript> {
        return compiledScripts.get(scriptFile) {
            try {
                LOGGER.trace("Compiling script file: {}", scriptFile)
                val compileStartMs = System.currentTimeMillis()
                val wrapped = JavaScriptUtil.wrapScript(scriptFile)

                val compiled = try {
                    CompiledJsScript<CompiledScript>(
                        preScriptLength = wrapped.preScriptLength,
                        code = scriptEngine.compile(wrapped.script),
                    )
                } catch (e: ScriptException) {
                    throw JavaScriptUtil.unwrapScriptException(e, wrapped)
                }

                LOGGER.debug("Script: {} compiled in {}ms", scriptFile, System.currentTimeMillis() - compileStartMs)
                return@get compiled

            } catch (e: Exception) {
                throw RuntimeException("Failed to compile script: $scriptFile", e)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(NashornStandaloneScriptServiceImpl::class.java)
        const val METRIC_SCRIPT_JS_CACHE_ENTRIES = "script.js.cache.entries"
    }
}
