package io.gatehill.imposter.service

import com.google.inject.Guice
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.ResponseBehaviourType
import org.junit.Before
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.*

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
abstract class AbstractScriptServiceImplTest {
    @Before
    void setUp() throws Exception {
        Guice.createInjector().injectMembers(this)
    }

    protected abstract ScriptService getService()

    protected abstract String getScriptName()

    private PluginConfig configureScript() {
        def script = Paths.get(AbstractScriptServiceImplTest.class.getResource("/script/${scriptName}").toURI())

        def config = new PluginConfigImpl()
        config.with {
            parentDir = script.parent.toFile()
            responseConfig.with {
                scriptFile = scriptName
            }
        }
        config
    }

    @Test
    void testExecuteScript_Immediate() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def bindings = [
                'hello': 'world'
        ]
        def actual = service.executeScript(pluginConfig, resourceConfig, bindings)

        assertNotNull actual
        assertEquals 201, actual.statusCode
        assertEquals 'foo.bar', actual.responseFile
        assertEquals ResponseBehaviourType.IMMEDIATE_RESPONSE, actual.behaviourType
        assertEquals 1, actual.getResponseHeaders().size()
        assertTrue actual.getResponseHeaders().containsKey("MyHeader")
        assertEquals "AwesomeHeader", actual.getResponseHeaders().get("MyHeader")
    }

    @Test
    void testExecuteScript_Default() throws Exception {
        def config = configureScript()
        def pluginConfig = config as PluginConfig
        def resourceConfig = config as ResourceConfig

        def bindings = [
                'hello': 'should not match'
        ]
        def actual = service.executeScript(pluginConfig, resourceConfig, bindings)

        assertNotNull actual
        assertEquals 200, actual.statusCode
        assertNull actual.responseFile
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
    }
}
