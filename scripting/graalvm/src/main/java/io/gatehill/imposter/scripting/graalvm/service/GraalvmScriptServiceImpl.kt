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
package io.gatehill.imposter.scripting.graalvm.service

import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.scripting.graalvm.GraalvmScriptingModule
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import javax.inject.Inject

/**
 * Graal implementation of JavaScript scripting engine.
 *
 * @author Pete Cornish
 */
@PluginInfo("js-graal")
@RequireModules(GraalvmScriptingModule::class)
class GraalvmScriptServiceImpl @Inject constructor() : ScriptService, Plugin {

    init {
        // see https://www.graalvm.org/reference-manual/js/NashornMigrationGuide/#nashorn-compatibility-mode
        System.setProperty("polyglot.js.nashorn-compat", "true")

//        // quieten interpreter mode warning until native graal compiler included in module path - see:
//        // https://www.graalvm.org/reference-manual/js/RunOnJDK/
//        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
    }

    override fun executeScript(
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        val scriptFile = ScriptUtil.resolveScriptPath(pluginConfig, resourceConfig.responseConfig.scriptFile)
        LOGGER.trace("Executing script file: {}", scriptFile)

        return try {
            val globals = JavaScriptUtil.transformRuntimeMap(runtimeContext, false)
            val wrappedScript = JavaScriptUtil.wrapScript(scriptFile)
            executeGraalJs(wrappedScript, globals, scriptFile.fileName.toString())
        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    private fun executeGraalJs(
        wrappedScript: String,
        globals: Map<String, *>,
        scriptName: String
    ): ReadWriteResponseBehaviour {

        val resultClone = ReadWriteResponseBehaviourImpl()

        Context.newBuilder("js").allowAllAccess(true).build().use { context ->
            val bindings = context.getBindings("js")
            globals.forEach { (key, value) -> bindings.putMember(key, value) }
            val source = Source.newBuilder("js", wrappedScript, scriptName).build()

            context.eval(source).`as`(ReadWriteResponseBehaviour::class.java).also { result ->
                resultClone.behaviourType = result.behaviourType
                resultClone.exampleName = result.exampleName
                resultClone.isTemplate = result.isTemplate
                resultClone.performanceSimulation = result.performanceSimulation
                resultClone.responseData = result.responseData
                resultClone.responseFile = result.responseFile
                resultClone.responseHeaders.putAll(result.responseHeaders)
                resultClone.statusCode = result.statusCode
            }
        }

        return resultClone
    }

    companion object {
        private val LOGGER = LogManager.getLogger(GraalvmScriptServiceImpl::class.java)
    }
}