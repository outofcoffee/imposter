/*
 * Copyright (c) 2016-2024.
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

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.dsl.Dsl
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.scripting.graalvm.GraalvmScriptingModule
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.service.ScriptSource
import org.apache.logging.log4j.LogManager
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.SimpleBindings

/**
 * Graal implementation of JavaScript scripting engine.
 *
 * @author Pete Cornish
 */
@PluginInfo("js-graal")
@RequireModules(GraalvmScriptingModule::class)
class GraalvmScriptServiceImpl : ScriptService, Plugin {
    private var scriptEngine: ScriptEngine

    init {
        // see https://www.graalvm.org/reference-manual/js/NashornMigrationGuide/#nashorn-compatibility-mode
        System.setProperty("polyglot.js.nashorn-compat", "true")

        // quieten interpreter mode warning until native graal compiler included in module path - see:
        // https://www.graalvm.org/reference-manual/js/RunOnJDK/
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")

        scriptEngine = GraalJSEngineFactory().scriptEngine
    }

    override fun executeScript(
        script: ScriptSource,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        LOGGER.trace("Executing script: {}", script)

        val bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)
        bindings["polyglot.js.allowAllAccess"] = true

        return try {
            val globals = JavaScriptUtil.transformRuntimeMap(
                runtimeContext,
                addDslPrefix = true,
                addConsoleShim = false
            )
            val wrapped = JavaScriptUtil.wrapScript(script)
            val result = scriptEngine.eval(wrapped.code, SimpleBindings(globals)) as Dsl
            result.responseBehaviour

        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(GraalvmScriptServiceImpl::class.java)
    }
}
