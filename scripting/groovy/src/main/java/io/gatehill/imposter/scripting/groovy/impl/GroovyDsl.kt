package io.gatehill.imposter.scripting.groovy.impl

import groovy.lang.Closure
import groovy.lang.Script
import io.gatehill.imposter.script.MutableResponseBehaviour
import io.gatehill.imposter.script.dsl.Dsl
import io.gatehill.imposter.script.dsl.DslImpl
import io.gatehill.imposter.scripting.groovy.util.ScriptLoader
import java.nio.file.Path

abstract class GroovyDsl : Script(), Dsl {
    private val dsl = DslImpl()

    override val responseBehaviour
        get() = dsl.responseBehaviour

    override fun respond() = dsl.respond()

    override fun newRequest() = dsl.newRequest()

    /**
     * Syntactic sugar that executes the closure immediately.
     *
     * @return `this`
     */
    fun respond(closure: Closure<*>): MutableResponseBehaviour {
        closure.delegate = dsl.responseBehaviour
        closure.call()
        return respond()
    }

    fun loadDynamic(relativePath: String): Any {
        val thisScriptPath = super.getProperty(ScriptLoader.contextKeyScriptPath) as Path
        return ScriptLoader.loadDynamic(thisScriptPath, relativePath)
    }
}
