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

package io.gatehill.imposter.scripting.common.util

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.scripting.common.dsl.RunnableDsl
import io.gatehill.imposter.scripting.common.shim.ConsoleShim
import io.gatehill.imposter.service.ScriptSource
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.script.ScriptException
import kotlin.io.path.readText


/**
 * @author Pete Cornish
 */
object JavaScriptUtil {
    private val LOGGER: Logger = LogManager.getLogger(JavaScriptUtil::class.java)
    private const val envJsPlugin = "IMPOSTER_JS_PLUGIN"

    private const val DSL_OBJECT_PREFIX = "__imposter_dsl_"
    private val globals = listOf(
        "context",
        "env",
        "logger",
        "respond",
        "stores",
    )

    private val DSL_FUNCTIONS: String
    private val GLOBAL_DSL_OBJECTS: String

    init {
        // expose superclass methods as DSL functions
        val dslMethods = listOf(
            "respond",
        )
        DSL_FUNCTIONS = dslMethods.distinct().joinToString("\r\n") { methodName ->
            "${DSL_OBJECT_PREFIX}${methodName} = Java.super(__dsl).${methodName};"
        }

        // optionally expose as global objects
        GLOBAL_DSL_OBJECTS = globals.distinct().joinToString("\r\n") { methodName ->
            "$methodName = ${DSL_OBJECT_PREFIX}${methodName};"
        }
    }

    /**
     * @return the plugin name of the active JavaScript implementation
     */
    val activePlugin: String
        get() = EnvVars.getEnv(envJsPlugin) ?: "js-nashorn"

    fun transformRuntimeMap(runtimeContext: RuntimeContext, addDslPrefix: Boolean, addConsoleShim: Boolean): Map<String, *> {
        val runtimeObjects = runtimeContext.asMap().toMutableMap()
        if (!runtimeObjects.containsKey("stores")) {
            runtimeObjects["stores"] = Any()
        }
        if (addConsoleShim) {
            runtimeObjects["console"] = ConsoleShim(runtimeObjects)
        }
        return runtimeObjects
            .mapKeys { if (addDslPrefix && globals.contains(it.key)) DSL_OBJECT_PREFIX + it.key else it.key }
    }

    fun wrapScript(script: ScriptSource): WrappedScript {
        val scriptCode = try {
            when (script.type) {
                ScriptSource.ScriptType.File -> script.file?.readText()!!
                ScriptSource.ScriptType.Inline -> script.code!!
                else -> throw UnsupportedOperationException("Unsupported script type: $script")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read script: $script", e)
        }

        val setGlobalDslObjects = !scriptCode.contains("@imposter-js/types")
        val wrappedScript = buildWrappedScript(scriptCode, setGlobalDslObjects)

        if (LOGGER.isTraceEnabled) {
            LOGGER.trace("Wrapped script: {}", wrappedScript)
        }
        return wrappedScript
    }

    private fun buildWrappedScript(script: String, setGlobalDslObjects: Boolean): WrappedScript {
        val preScript = """
var RunnableDsl = Java.type('${RunnableDsl::class.java.canonicalName}');

var __dsl = new RunnableDsl() {
    run: function() {

/* ------------------------------------------------------------------------- */
/* DSL functions                                                             */
/* ------------------------------------------------------------------------- */
$DSL_FUNCTIONS
${if (setGlobalDslObjects) GLOBAL_DSL_OBJECTS else ""}
/* ------------------------------------------------------------------------- */
/* Shim for '__imposter_types' module exports                                */
/* ------------------------------------------------------------------------- */
var __imposter_types = {
    env: (function() { return ${DSL_OBJECT_PREFIX}env })(),
    context: (function() { return ${DSL_OBJECT_PREFIX}context })(),
    logger: (function() { return ${DSL_OBJECT_PREFIX}logger })(),
    respond: ${DSL_OBJECT_PREFIX}respond,
    stores: (function() { try { return ${DSL_OBJECT_PREFIX}stores } catch(e) { return undefined } })()
};
/* ------------------------------------------------------------------------- */
/* Shim for 'require()'                                                      */
/* ------------------------------------------------------------------------- */
function require(moduleName) {
  if ("@imposter-js/types" !== moduleName){
    throw new Error('require() only supports "@imposter-js/types"');
  }
  return __imposter_types;
}
/* ------------------------------------------------------------------------- */
/* Mock script                                                               */
/* ------------------------------------------------------------------------- */
"""
        val postScript = """
/* ------------------------------------------------------------------------- */

    }
}

__dsl.run();
__dsl;
"""
        return WrappedScript(preScript.lines().size, preScript + script + postScript)
    }

    /**
     * Adjust line number to allow for script wrapper.
     */
    fun unwrapScriptException(e: ScriptException, meta: ScriptMetadata): ScriptException {
        val lineNumber = e.lineNumber - meta.preScriptLength + 1
        return ScriptException(
            e.cause?.message ?: e.message, e.fileName, lineNumber, e.columnNumber
        )
    }
}
