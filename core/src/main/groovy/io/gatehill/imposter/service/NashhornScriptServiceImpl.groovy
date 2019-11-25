package io.gatehill.imposter.service

import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.InternalResponseBehavior
import io.gatehill.imposter.script.MutableResponseBehaviour
import io.gatehill.imposter.script.impl.RunnableResponseBehaviourImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.inject.Inject
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class NashhornScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(NashhornScriptServiceImpl.class);

    @Inject
    private ScriptEngineManager scriptEngineManager;

    @Override
    InternalResponseBehavior executeScript(PluginConfig pluginConfig, ResourceConfig resourceConfig, Map<String, Object> bindings) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");

        try {
            return (InternalResponseBehavior) scriptEngine.eval(wrapScript(scriptFile), new SimpleBindings(bindings));

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }

    private static String wrapScript(Path scriptFile) throws IOException {
        // expose DSL methods
        def dslMethods = []
        MutableResponseBehaviour.class.declaredMethods.each { method -> dslMethods += method.name }

        def dslFunctions = ""
        dslMethods.unique().each {
            methodName -> dslFunctions += "var ${methodName} = Java.super(responseBehaviour).${methodName};\r\n"
        }

        // wrap mock script
        def mockScript = new String(Files.readAllBytes(scriptFile))
        def wrappedScript = buildWrappedScript(dslFunctions, mockScript)

        LOGGER.trace("Wrapped script: ${wrappedScript}")
        return wrappedScript
    }

    private static String buildWrappedScript(dslFunctions, mockScript) {
        """
var RunnableResponseBehaviourImpl = Java.type('${RunnableResponseBehaviourImpl.class.canonicalName}');

var responseBehaviour = new RunnableResponseBehaviourImpl() {
    run: function() {

/* ------------------------------------------------------------------------- */
/* Exposed DSL functions                                                     */
/* ------------------------------------------------------------------------- */
${dslFunctions}
/* ------------------------------------------------------------------------- */
/* Mock script                                                               */
/* ------------------------------------------------------------------------- */
${mockScript}
/* ------------------------------------------------------------------------- */

    }
}

responseBehaviour.run();

/* return the configured behaviour */
responseBehaviour;
"""
    }
}
