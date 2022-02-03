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
package io.gatehill.imposter.scripting.groovy.service

import com.google.common.cache.CacheBuilder
import groovy.lang.Binding
import groovy.lang.GroovyClassLoader
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.scripting.groovy.impl.GroovyResponseBehaviourImpl
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.MetricsUtil
import io.micrometer.core.instrument.Gauge
import org.apache.logging.log4j.LogManager
import org.codehaus.groovy.control.CompilerConfiguration
import java.nio.file.Path

/**
 * @author Pete Cornish
 */
class GroovyScriptServiceImpl : ScriptService {
    private val groovyClassLoader: GroovyClassLoader

    /**
     * Holds compiled scripts, with maximum number of entries determined by the environment
     * variable [ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES].
     */
    private val scriptClasses = CacheBuilder.newBuilder()
        .maximumSize(EnvVars.getEnv(ScriptUtil.ENV_SCRIPT_CACHE_ENTRIES)?.toLong() ?: ScriptUtil.DEFAULT_SCRIPT_CACHE_ENTRIES)
        .build<Path, Class<GroovyResponseBehaviourImpl>>()

    init {
        val compilerConfig = CompilerConfiguration()
        compilerConfig.scriptBaseClass = GroovyResponseBehaviourImpl::class.java.canonicalName
        groovyClassLoader = GroovyClassLoader(this::class.java.classLoader, compilerConfig)

        MetricsUtil.doIfMetricsEnabled(METRIC_SCRIPT_GROOVY_CACHE_ENTRIES) { registry ->
            Gauge.builder(METRIC_SCRIPT_GROOVY_CACHE_ENTRIES) { scriptClasses.size() }
                .description("The number of cached Groovy files")
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
        resourceConfig: BasicResourceConfig,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        val scriptFile = ScriptUtil.resolveScriptPath(pluginConfig, resourceConfig.responseConfig.scriptFile)
        LOGGER.trace("Executing script file: {}", scriptFile)

        return try {
            val scriptClass = getCompiledScript(scriptFile)
            val script = scriptClass.getDeclaredConstructor().newInstance()
            script.apply {
                binding = convertBindings(runtimeContext)
                run()
            }
        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    private fun getCompiledScript(scriptFile: Path): Class<GroovyResponseBehaviourImpl> {
        return scriptClasses.get(scriptFile) {
            try {
                LOGGER.trace("Compiling script file: {}", scriptFile)
                val compileStartMs = System.currentTimeMillis()

                @Suppress("UNCHECKED_CAST")
                val compiled = groovyClassLoader.parseClass(scriptFile.toFile()) as Class<GroovyResponseBehaviourImpl>

                LOGGER.debug("Script: {} compiled in {}ms", scriptFile, System.currentTimeMillis() - compileStartMs)
                return@get compiled

            } catch (e: Exception) {
                throw RuntimeException("Failed to load Groovy script: $scriptFile", e)
            }
        }
    }

    private fun convertBindings(runtimeContext: RuntimeContext) = Binding().apply {
        runtimeContext.asMap().forEach { (name: String, value: Any?) -> setVariable(name, value) }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(GroovyScriptServiceImpl::class.java)
        const val METRIC_SCRIPT_GROOVY_CACHE_ENTRIES = "script.groovy.cache.entries"
    }
}
