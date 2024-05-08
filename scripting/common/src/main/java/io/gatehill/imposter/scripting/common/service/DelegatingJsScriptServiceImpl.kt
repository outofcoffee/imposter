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
package io.gatehill.imposter.scripting.common.service

import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.service.ScriptRequestBuilder
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.service.ScriptSource
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * A delegating JavaScript script service that uses the environment variable named by [JavaScriptUtil.envJsPlugin]
 * to determine the [ScriptService] to use.
 *
 * @author Pete Cornish
 */
class DelegatingJsScriptServiceImpl @Inject constructor(
    private val pluginManager: PluginManager,
) : ScriptService {
    private val logger = LogManager.getLogger(DelegatingJsScriptServiceImpl::class.java)

    private val impl: ScriptService by lazy { loadJsImpl() }

    private fun loadJsImpl(): ScriptService {
        val jsPlugin = JavaScriptUtil.activePlugin
        val pluginClass = pluginManager.determinePluginClass(jsPlugin)
        logger.trace("Resolved JavaScript plugin: {} to class: {}", jsPlugin, pluginClass)

        try {
            val plugin = pluginManager.getPlugin<Plugin>(pluginClass)
                ?: throw IllegalStateException("Unable to load JavaScript plugin: $pluginClass")

            return plugin as ScriptService

        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to load JavaScript plugin: $jsPlugin. Must be an installed plugin implementing ${ScriptService::class.java.canonicalName}",
                e
            )
        }
    }

    override val requestBuilder: ScriptRequestBuilder
        get() = impl.requestBuilder

    override fun initScript(script: ScriptSource) = impl.initScript(script)

    override fun initInlineScript(scriptId: String, scriptCode: String) = impl.initInlineScript(scriptId, scriptCode)

    override fun executeScript(
        script: ScriptSource,
        runtimeContext: RuntimeContext
    ) = impl.executeScript(script, runtimeContext)

    override fun evalInlineScript(
        scriptId: String,
        scriptCode: String,
        runtimeContext: RuntimeContext
    ) = impl.evalInlineScript(scriptId, scriptCode, runtimeContext)
}
