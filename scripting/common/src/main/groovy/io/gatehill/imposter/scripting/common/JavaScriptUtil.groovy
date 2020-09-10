package io.gatehill.imposter.scripting.common

import io.gatehill.imposter.script.MutableResponseBehaviour
import io.gatehill.imposter.script.impl.RunnableResponseBehaviourImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author pete
 */
class JavaScriptUtil {
    private static final Logger LOGGER = LogManager.getLogger(JavaScriptUtil.class);

    private JavaScriptUtil() {}

    static String wrapScript(Path scriptFile) throws IOException {
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
