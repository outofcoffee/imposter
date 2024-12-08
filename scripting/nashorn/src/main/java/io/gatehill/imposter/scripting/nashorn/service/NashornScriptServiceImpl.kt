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
package io.gatehill.imposter.scripting.nashorn.service

import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.model.script.LazyContextBuilder
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.ScriptBindings
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.script.dsl.Dsl
import io.gatehill.imposter.script.dsl.FunctionHolder
import io.gatehill.imposter.scripting.common.util.CompiledJsScript
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.service.ScriptSource
import io.gatehill.imposter.util.MetricsUtil.doIfMetricsEnabled
import io.gatehill.imposter.util.getJvmVersion
import io.micrometer.core.instrument.Gauge
import org.apache.logging.log4j.LogManager
import org.openjdk.nashorn.api.scripting.NashornScriptEngine
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import javax.script.CompiledScript
import javax.script.ScriptException
import javax.script.SimpleBindings

/**
 * Standalone Nashorn implementation of JavaScript scripting engine
 * for JDK 11-14.
 *
 * @author Pete Cornish
 */
@PluginInfo("js-nashorn")
@RequireModules(NashornScriptingModule::class)
class NashornScriptServiceImpl : ScriptService, Plugin {
    private val scriptEngine: NashornScriptEngine

    override val implName = "js-nashorn"

    /**
     * Holds compiled scripts, with maximum number of entries determined by the environment
     * variable [ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES].
     */
    private val compiledScripts = CacheBuilder.newBuilder()
        .maximumSize(getEnv(ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES)?.toLong() ?: ScriptUtil.DEFAULT_SCRIPT_CACHE_ENTRIES)
        .build<String, CompiledJsScript<CompiledScript>>()

    override val contextBuilder = LazyContextBuilder

    init {
        if (getJvmVersion() < 11) {
            throw UnsupportedOperationException("Standalone Nashorn JavaScript plugin is only supported on Java 11+.")
        }

        scriptEngine = NashornScriptEngineFactory().scriptEngine as NashornScriptEngine

        doIfMetricsEnabled(METRIC_SCRIPT_JS_CACHE_ENTRIES) { registry ->
            Gauge.builder(METRIC_SCRIPT_JS_CACHE_ENTRIES) { compiledScripts.size() }
                .description("The number of cached compiled JavaScript scripts")
                .register(registry)
        }
    }

    override fun initScript(script: ScriptSource) {
        if (ScriptUtil.shouldPrecompile) {
            LOGGER.debug("Precompiling script: {}", script)
            getCompiledScript(script)
        }
    }

    override fun initEvalScript(scriptId: String, scriptCode: String) {
        if (ScriptUtil.shouldPrecompile) {
            LOGGER.debug("Precompiling inline script: $scriptId")
            getCompiledInlineScript(scriptId, scriptCode)
        }
    }

    override fun executeScript(
        script: ScriptSource,
        scriptBindings: ScriptBindings
    ): ReadWriteResponseBehaviour {
        LOGGER.trace("Evaluating script: {}", script)
        check(script.valid) { "Invalid script: $script" }

        try {
            val bindings = SimpleBindings(
                JavaScriptUtil.transformBindingsMap(
                    scriptBindings,
                    addDslPrefix = true,
                    addConsoleShim = true
                )
            )

            val compiled = getCompiledScript(script)
            try {
                val fnHolder = compiled.code.eval(bindings) as FunctionHolder

                LOGGER.trace("Invoking script code: {}", script)
                fnHolder.run()

                val result = bindings[JavaScriptUtil.DSL_VAR_NAME] as Dsl
                return result.responseBehaviour

            } catch (e: ScriptException) {
                throw JavaScriptUtil.unwrapScriptException(e, compiled)
            }

        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    override fun executeEvalScript(
        scriptId: String,
        scriptCode: String,
        scriptBindings: ScriptBindings
    ): Boolean {
        LOGGER.trace("Executing eval script: {}", scriptId)

        try {
            val bindings = SimpleBindings(
                JavaScriptUtil.transformBindingsMap(
                    scriptBindings,
                    addDslPrefix = false,
                    addConsoleShim = false
                )
            )

            val compiled = getCompiledInlineScript(scriptId, scriptCode)
            val result = compiled.code.eval(bindings)
            return result is Boolean && result

        } catch (e: Exception) {
            throw RuntimeException("Eval script execution terminated abnormally", e)
        }
    }

    private fun getCompiledScript(script: ScriptSource): CompiledJsScript<CompiledScript> =
        compiledScripts.get(script.source) {
            try {
                LOGGER.trace("Compiling script: {}", script)
                val compileStartMs = System.currentTimeMillis()

                val wrapped = JavaScriptUtil.wrapScript(script)
                val compiled = try {
                    CompiledJsScript<CompiledScript>(
                        preScriptLength = wrapped.preScriptLength,
                        code = scriptEngine.compile(wrapped.code),
                    )
                } catch (e: ScriptException) {
                    throw JavaScriptUtil.unwrapScriptException(e, wrapped)
                }

                LOGGER.debug("Script: {} compiled in {}ms", script, System.currentTimeMillis() - compileStartMs)
                return@get compiled

            } catch (e: Exception) {
                throw RuntimeException("Failed to compile script: $script", e)
            }
        }

    private fun getCompiledInlineScript(
        scriptId: String,
        scriptCode: String,
    ): CompiledJsScript<CompiledScript> =
        compiledScripts.get(scriptId) {
            try {
                LOGGER.trace("Compiling eval script: {}", scriptCode)
                return@get CompiledJsScript(0, scriptEngine.compile(scriptCode))
            } catch (e: Exception) {
                throw RuntimeException("Failed to compile eval script: $scriptCode", e)
            }
        }

    companion object {
        private val LOGGER = LogManager.getLogger(NashornScriptServiceImpl::class.java)
        const val METRIC_SCRIPT_JS_CACHE_ENTRIES = "script.js.cache.entries"
    }
}
