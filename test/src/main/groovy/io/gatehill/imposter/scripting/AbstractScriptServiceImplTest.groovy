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
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
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
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * @author Pete Cornish
 */
abstract class AbstractScriptServiceImplTest {
    @BeforeClass
    static void beforeClass() throws Exception {
        FeatureUtil.disableFeature(MetricsUtil.FEATURE_NAME_METRICS)
    }

    @AfterClass
    static void afterClass() throws Exception {
        FeatureUtil.clearSystemPropertyOverrides()
    }

    @Before
    void setUp() throws Exception {
        Guice.createInjector().injectMembers(this)
    }

    protected abstract ScriptService getService()

    protected abstract String getScriptName()

    protected PluginConfig configureScript() {
        def script = Paths.get(AbstractScriptServiceImplTest.class.getResource("/script/${scriptName}").toURI())

        def config = new PluginConfigImpl()
        config.with {
            parentDir = script.parent.toFile()
            responseConfig.with {
                it.scriptFile = scriptName
            }
        }
        return config
    }

    protected RuntimeContext buildRuntimeContext(
            Map<String, String> additionalBindings,
            Map<String, String> headers = [:],
            Map<String, String> pathParams = [:],
            Map<String, String> queryParams = [:],
            Map<String, String> env = [:]
    ) {
        def logger = LogManager.getLogger('script-engine-test')

        def mockRequest = mock(HttpServerRequest.class)
        when(mockRequest.method()).thenReturn(HttpMethod.GET)
        when(mockRequest.path()).thenReturn('/example')
        when(mockRequest.absoluteURI()).thenReturn('http://localhost:8080/example')
        when(mockRequest.headers()).thenReturn(new CaseInsensitiveHeaders().addAll(headers))
        when(mockRequest.params()).thenReturn(new CaseInsensitiveHeaders().addAll(queryParams))

        def mockRoutingContext = mock(RoutingContext.class)
        when(mockRoutingContext.request()).thenReturn(mockRequest)
        when(mockRoutingContext.pathParams()).thenReturn(pathParams)
        when(mockRoutingContext.getBodyAsString()).thenReturn('')

        def pluginConfig = mock(PluginConfig.class)
        def executionContext = ScriptUtil.buildContext(mockRoutingContext, null)
        def runtimeContext = new RuntimeContext(env, logger, pluginConfig, additionalBindings, executionContext)
        return runtimeContext
    }

    @Test
    void testExecuteScript_Immediate() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]
        def runtimeContext = buildRuntimeContext(additionalBindings)
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 201, actual.statusCode
        assertEquals 'foo.bar', actual.responseFile
        assertEquals ResponseBehaviourType.SHORT_CIRCUIT, actual.behaviourType
        assertEquals 1, actual.getResponseHeaders().size()
        assertEquals "AwesomeHeader", actual.getResponseHeaders().get("MyHeader")
    }

    @Test
    void testExecuteScript_Default() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'should not match'
        ]
        def runtimeContext = buildRuntimeContext(additionalBindings)
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        // zero as un-set by script
        assertEquals 0, actual.statusCode
        assertNull actual.responseFile
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
    }

    @Test
    void testExecuteScript_ParsePathParams() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]
        def pathParams = ['qux': 'quux']

        RuntimeContext runtimeContext = buildRuntimeContext(additionalBindings, [:], pathParams, [:], [:])
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 203, actual.statusCode
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
        assertEquals 'quux', actual.getResponseHeaders().get('X-Echo-Qux')
    }

    @Test
    void testExecuteScript_ParseQueryParams() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]
        def queryParams = ['foo': 'bar']

        RuntimeContext runtimeContext = buildRuntimeContext(additionalBindings, [:], [:], queryParams, [:])
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 200, actual.statusCode
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
        assertEquals 'bar', actual.getResponseHeaders().get('X-Echo-Foo')
    }

    @Test
    void testExecuteScript_ParseRequestHeaders() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]
        def headers = ['baz': 'qux']

        RuntimeContext runtimeContext = buildRuntimeContext(additionalBindings, headers, [:], [:], [:])
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 202, actual.statusCode
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
        assertEquals 'qux', actual.getResponseHeaders().get('X-Echo-Baz')
    }

    @Test
    void testExecuteScript_ParseNormalisedRequestHeaders() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]

        // request header casing should be normalised by the script engine
        def headers = ['CORGE': 'grault']

        RuntimeContext runtimeContext = buildRuntimeContext(additionalBindings, headers, [:], [:], [:])
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 202, actual.statusCode
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
        assertEquals 'grault', actual.getResponseHeaders().get('X-Echo-Corge')
    }

    @Test
    void testExecuteScript_ReadEnvironmentVariable() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def additionalBindings = [
                'hello': 'world'
        ]
        // override environment
        def env = [
                'example': 'foo'
        ]
        RuntimeContext runtimeContext = buildRuntimeContext(additionalBindings, [:], [:], [:], env)
        def actual = service.executeScript(pluginConfig, resourceConfig, runtimeContext)

        assertNotNull actual
        assertEquals 204, actual.statusCode
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
        assertEquals 'foo', actual.getResponseHeaders().get('X-Echo-Env-Var')
    }
}
