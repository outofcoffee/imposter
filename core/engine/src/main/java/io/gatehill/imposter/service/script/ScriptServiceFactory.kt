/*
 * Copyright (c) 2021-2021.
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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.script.annotation.GroovyImpl
import io.gatehill.imposter.script.annotation.JavascriptImpl
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import java.util.*
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ScriptServiceFactory {
    @Inject
    @GroovyImpl
    private lateinit var groovyScriptService: ScriptService

    @Inject
    @JavascriptImpl
    private lateinit var javascriptScriptService: ScriptService

    @Inject
    private lateinit var embeddedScriptService: EmbeddedScriptService

    @Inject
    private lateinit var imposterConfig: ImposterConfig

    fun fetchScriptService(scriptSource: String): ScriptService {
        if (imposterConfig.useEmbeddedScriptEngine) {
            LOGGER.debug("Using embedded script engine")
            return embeddedScriptService
        }

        val scriptExtension: String
        val dotIndex = scriptSource.lastIndexOf('.')
        scriptExtension = if (dotIndex >= 1 && dotIndex < scriptSource.length - 1) {
            scriptSource.substring(dotIndex + 1)
        } else {
            ""
        }
        return when (scriptExtension.lowercase(Locale.getDefault())) {
            "groovy" -> groovyScriptService
            "js" -> javascriptScriptService
            else -> throw RuntimeException("Unable to determine script engine from script file name: $scriptSource")
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ScriptServiceFactory::class.java)
    }
}