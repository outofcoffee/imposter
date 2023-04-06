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

package io.gatehill.imposter.scripting

import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.util.HttpServerUtil
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

/**
 * @author Pete Cornish
 */
abstract class AbstractBaseScriptHttpTest : AbstractBaseScriptTest() {
    @Test
    fun `make HTTP request`() {
        val listenPort = HttpServerUtil.listen(vertx!!) { request ->
            request.response().end("Hello, world!")
        }

        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as BasicResourceConfig

        val additionalBindings = mapOf(
            "url" to "http://localhost:$listenPort"
        )
        val runtimeContext = buildRuntimeContext(additionalBindings, emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val scriptPath = ScriptUtil.resolveScriptPath(pluginConfig, resourceConfig.responseConfig.scriptFile)
        val actual = getService().executeScript(scriptPath, runtimeContext)

        assertEquals("Hello, world!", actual.content)
    }

    companion object {
        private var vertx: Vertx? = null

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            vertx = Vertx.vertx()
        }

        @JvmStatic
        @AfterClass
        @Throws(Exception::class)
        fun afterClass() {
            HttpServerUtil.blockWait<AsyncResult<Void>?> { completionHandler -> vertx!!.close(completionHandler) }
        }
    }
}
