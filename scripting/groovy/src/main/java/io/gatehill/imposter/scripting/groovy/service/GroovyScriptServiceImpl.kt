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

import groovy.lang.Binding
import groovy.lang.GroovyCodeSource
import groovy.lang.GroovyShell
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.scripting.groovy.impl.GroovyResponseBehaviourImpl
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import org.codehaus.groovy.control.CompilerConfiguration
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
class GroovyScriptServiceImpl : ScriptService {
    override fun executeScript(
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder?,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        val scriptFile = Paths.get(pluginConfig.parentDir.absolutePath, resourceConfig!!.responseConfig.scriptFile)
        LOGGER.trace("Executing script file: {}", scriptFile)

        val compilerConfig = CompilerConfiguration()
        compilerConfig.scriptBaseClass = GroovyResponseBehaviourImpl::class.java.canonicalName

        val groovyShell = GroovyShell(convertBindings(runtimeContext), compilerConfig)
        return try {
            (groovyShell.parse(
                GroovyCodeSource(scriptFile.toFile(), compilerConfig.sourceEncoding)
            ) as GroovyResponseBehaviourImpl).apply {
                run()
            }
        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    private fun convertBindings(runtimeContext: RuntimeContext) = Binding().apply {
        runtimeContext.asMap().forEach { (name: String, value: Any?) -> setVariable(name, value) }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(GroovyScriptServiceImpl::class.java)
    }
}