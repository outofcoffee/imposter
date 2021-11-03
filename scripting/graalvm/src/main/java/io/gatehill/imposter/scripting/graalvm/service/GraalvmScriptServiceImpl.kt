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

import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.scripting.common.JavaScriptUtil.wrapScript
import io.gatehill.imposter.scripting.graalvm.service.GraalvmScriptServiceImpl
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths
import javax.inject.Inject
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * @author Pete Cornish
 */
class GraalvmScriptServiceImpl @Inject constructor(
    private val scriptEngineManager: ScriptEngineManager
) : ScriptService {

    init {
        // see https://www.graalvm.org/reference-manual/js/NashornMigrationGuide/#nashorn-compatibility-mode
        System.setProperty("polyglot.js.nashorn-compat", "true")

        // quieten interpreter mode warning until native graal compiler included in module path - see:
        // https://www.graalvm.org/reference-manual/js/RunOnJDK/
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
    }

    override fun executeScript(
        pluginConfig: PluginConfig,
        resourceConfig: ResponseConfigHolder?,
        runtimeContext: RuntimeContext
    ): ReadWriteResponseBehaviour {
        val scriptFile = Paths.get(pluginConfig.parentDir.absolutePath, resourceConfig!!.responseConfig.scriptFile)
        LOGGER.trace("Executing script file: {}", scriptFile)

        val scriptEngine = scriptEngineManager.getEngineByName("graal.js")
        val bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)
        bindings["polyglot.js.allowAllAccess"] = true

        return try {
            scriptEngine.eval(
                wrapScript(scriptFile),
                SimpleBindings(runtimeContext.asMap())
            ) as ReadWriteResponseBehaviour
        } catch (e: Exception) {
            throw RuntimeException("Script execution terminated abnormally", e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(GraalvmScriptServiceImpl::class.java)
    }
}