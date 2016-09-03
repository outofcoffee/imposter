package com.gatehill.imposter.service

import com.gatehill.imposter.plugin.config.ResourceConfig
import com.gatehill.imposter.script.ResponseBehaviourType
import com.google.inject.Guice
import org.junit.Before
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.*

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractScriptServiceImplTest {
    @Before
    public void setUp() throws Exception {
        Guice.createInjector().injectMembers(this)
    }

    protected abstract ScriptService getService()

    protected abstract String getScriptName()

    private ResourceConfig configureScript() {
        def script = Paths.get(AbstractScriptServiceImplTest.class.getResource("/script/${scriptName}").toURI())

        def config = new ResourceConfig()
        config.with {
            parentDir = script.parent.toFile()
            responseConfig.with {
                scriptFile = scriptName
            }
        }
        config
    }

    @Test
    public void testExecuteScript_Immediate() throws Exception {
        def config = configureScript()

        def bindings = [
                'hello': 'world'
        ]
        def actual = service.executeScript(config, bindings)

        assertNotNull actual
        assertEquals 201, actual.statusCode
        assertEquals 'foo.bar', actual.responseFile
        assertEquals ResponseBehaviourType.IMMEDIATE_RESPONSE, actual.behaviourType
    }

    @Test
    public void testExecuteScript_Default() throws Exception {
        def config = configureScript()

        def bindings = [
                'hello': 'should not match'
        ]
        def actual = service.executeScript(config, bindings)

        assertNotNull actual
        assertEquals 200, actual.statusCode
        assertNull actual.responseFile
        assertEquals ResponseBehaviourType.DEFAULT_BEHAVIOUR, actual.behaviourType
    }
}
