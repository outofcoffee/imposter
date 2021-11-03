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

import com.google.inject.Guice
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.ResponseBehaviourType
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.FeatureUtil
import io.gatehill.imposter.util.MetricsUtil
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.mock
import java.nio.file.Paths
import org.mockito.Mockito.`when` as When

/**
 * @author Pete Cornish
 */
abstract class AbstractScriptServiceImplTest {
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
        Guice.createInjector().injectMembers(this)
    }

    protected abstract fun getService(): ScriptService

    protected abstract fun getScriptName(): String

    protected fun configureScript(): PluginConfig {
        val script =
            Paths.get(AbstractScriptServiceImplTest::class.java.getResource("/script/${getScriptName()}").toURI())

        return PluginConfigImpl().apply {
            parentDir = script.parent.toFile()
            responseConfig.apply {
                this.scriptFile = getScriptName()
            }
        }
    }

    protected fun buildRuntimeContext(
        additionalBindings: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        env: Map<String, String> = emptyMap()
    ): RuntimeContext {
        val logger = LogManager.getLogger("script-engine-test")

        val mockRequest = mock(HttpServerRequest::class.java)
        When(mockRequest.method()).thenReturn(HttpMethod.GET)
        When(mockRequest.path()).thenReturn("/example")
        When(mockRequest.absoluteURI()).thenReturn("http://localhost:8080/example")
        When(mockRequest.headers()).thenReturn(CaseInsensitiveHeaders().addAll(headers))
        When(mockRequest.params()).thenReturn(CaseInsensitiveHeaders().addAll(queryParams))

        val mockRoutingContext = mock(RoutingContext::class.java)
        When(mockRoutingContext.request()).thenReturn(mockRequest)
        When(mockRoutingContext.pathParams()).thenReturn(pathParams)
        When(mockRoutingContext.getBodyAsString()).thenReturn("")

        val pluginConfig = mock(PluginConfig::class.java)
        val executionContext = ScriptUtil.buildContext(mockRoutingContext, null)
        return RuntimeContext(env, logger, pluginConfig, additionalBindings, executionContext)
    }

    @Test
    fun testExecuteScript_Immediate() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )
        val runtimeContext = buildRuntimeContext(additionalBindings)
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(201, actual.statusCode)
        assertEquals("foo.bar", actual.responseFile)
        assertEquals(ResponseBehaviourType.SHORT_CIRCUIT, actual.behaviourType)
        assertEquals(1, actual.responseHeaders.size)
        assertEquals("AwesomeHeader", actual.responseHeaders.get("MyHeader"))
    }

    @Test
    fun testExecuteScript_Default() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "should not match"
        )
        val runtimeContext = buildRuntimeContext(additionalBindings)
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        // zero as un-set by script
        assertEquals(0, actual.statusCode)
        assertNull(actual.responseFile)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
    }

    @Test
    fun testExecuteScript_ParsePathParams() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )
        val pathParams = mapOf("qux" to "quux")

        val runtimeContext = buildRuntimeContext(additionalBindings, emptyMap(), pathParams, emptyMap(), emptyMap())
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(203, actual.statusCode)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
        assertEquals("quux", actual.responseHeaders.get("X-Echo-Qux"))
    }

    @Test
    fun testExecuteScript_ParseQueryParams() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )
        val queryParams = mapOf("foo" to "bar")

        val runtimeContext = buildRuntimeContext(additionalBindings, emptyMap(), emptyMap(), queryParams, emptyMap())
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(200, actual.statusCode)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
        assertEquals("bar", actual.responseHeaders.get("X-Echo-Foo"))
    }

    @Test
    fun testExecuteScript_ParseRequestHeaders() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )
        val headers = mapOf("baz" to "qux")

        val runtimeContext = buildRuntimeContext(additionalBindings, headers, emptyMap(), emptyMap(), emptyMap())
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(202, actual.statusCode)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
        assertEquals("qux", actual.responseHeaders.get("X-Echo-Baz"))
    }

    @Test
    fun testExecuteScript_ParseNormalisedRequestHeaders() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )

        // request header casing should be normalised by the script engine
        val headers = mapOf("CORGE" to "grault")

        val runtimeContext = buildRuntimeContext(additionalBindings, headers, emptyMap(), emptyMap(), emptyMap())
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(202, actual.statusCode)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
        assertEquals("grault", actual.responseHeaders.get("X-Echo-Corge"))
    }

    @Test
    fun testExecuteScript_ReadEnvironmentVariable() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as ResponseConfigHolder

        val additionalBindings = mapOf(
            "hello" to "world"
        )
        // override environment
        val env = mapOf(
            "example" to "foo"
        )
        val runtimeContext = buildRuntimeContext(additionalBindings, emptyMap(), emptyMap(), emptyMap(), env)
        val actual = getService().executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull(actual)
        assertEquals(204, actual.statusCode)
        assertEquals(ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType)
        assertEquals("foo", actual.responseHeaders.get("X-Echo-Env-Var"))
    }
}
