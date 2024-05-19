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

package io.gatehill.imposter.scripting

import com.google.inject.Module
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.service.ScriptSource
import io.gatehill.imposter.util.FeatureUtil
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.MetricsUtil
import org.apache.logging.log4j.LogManager
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import org.mockito.Mockito.`when` as When

/**
 * @author Pete Cornish
 */
abstract class AbstractBaseScriptTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FeatureUtil.disableFeature(MetricsUtil.FEATURE_NAME_METRICS)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            FeatureUtil.clearSystemPropertyOverrides()
        }
    }

    @Before
    fun setUp() {
        onBeforeInject()
        InjectorUtil.create(*modules).injectMembers(this)
    }

    protected abstract fun getService(): ScriptService

    protected open val modules: Array<out Module> = emptyArray()

    protected abstract fun getScriptName(): String

    protected open fun onBeforeInject() {}

    protected val fullScriptPath: Path
        get() {
            val relativePath = "/script/${getScriptName()}"
            try {
                return Paths.get(AbstractBaseScriptTest::class.java.getResource(relativePath).toURI())
            } catch (e: Exception) {
                throw RuntimeException("Failed to resolve path to script resource: $relativePath")
            }
        }

    protected fun configureScript(): PluginConfig {
        return PluginConfigImpl().apply {
            dir = fullScriptPath.parent.toFile()
            responseConfig.apply {
                this.scriptFile = getScriptName()
            }
        }
    }

    protected fun buildRuntimeContext(
        additionalBindings: Map<String, Any>,
        headers: Map<String, String> = emptyMap(),
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        env: Map<String, String> = emptyMap(),
        body: String = ""
    ): RuntimeContext {
        val logger = LogManager.getLogger("script-engine-test")

        val mockRequest = mock(HttpRequest::class.java)
        When(mockRequest.method).thenReturn(HttpMethod.GET)
        When(mockRequest.path).thenReturn("/example")
        When(mockRequest.absoluteUri).thenReturn("http://localhost:8080/example")
        When(mockRequest.headers).thenReturn(headers)
        When(mockRequest.pathParams).thenReturn(pathParams)
        When(mockRequest.queryParams).thenReturn(queryParams)
        When(mockRequest.bodyAsString).thenReturn(body)

        val mockHttpExchange = mock(HttpExchange::class.java)
        When(mockHttpExchange.request).thenReturn(mockRequest)

        val pluginConfig = mock(PluginConfig::class.java)
        val executionContext = ScriptUtil.buildContext(getService().requestBuilder, mockHttpExchange, null)
        return RuntimeContext(env, logger, pluginConfig, additionalBindings, executionContext)
    }

    protected fun resolveScriptFile(pluginConfig: PluginConfig, resourceConfig: BasicResourceConfig): ScriptSource {
        val scriptPath = ScriptUtil.resolveScriptPath(pluginConfig, resourceConfig.responseConfig.scriptFile)
        return ScriptSource(
            source = scriptPath.pathString,
            file = scriptPath,
        )
    }
}
