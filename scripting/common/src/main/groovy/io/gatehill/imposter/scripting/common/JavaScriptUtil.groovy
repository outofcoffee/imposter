package io.gatehill.imposter.scripting.common

import io.gatehill.imposter.script.MutableResponseBehaviour
import io.gatehill.imposter.script.impl.RunnableResponseBehaviourImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class JavaScriptUtil {
    private static final Logger LOGGER = LogManager.getLogger(JavaScriptUtil.class);

    private static final String DSL_FUNCTIONS = buildDslFunctions()

    private JavaScriptUtil() {}

    /**
     * Expose superclass methods as DSL functions.
     *
     * @return the JavaScript function variables
     */
    static String buildDslFunctions() {
        def dslMethods = MutableResponseBehaviour.class.declaredMethods.collect { method -> method.name }

        return dslMethods.unique()
                .collect { methodName -> "var ${methodName} = Java.super(responseBehaviour).${methodName};" }
                .join('\r\n')
    }

    static String wrapScript(Path scriptFile) throws IOException {
        // wrap mock script
        def mockScript = new String(Files.readAllBytes(scriptFile))
        def wrappedScript = buildWrappedScript(DSL_FUNCTIONS, mockScript)

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
