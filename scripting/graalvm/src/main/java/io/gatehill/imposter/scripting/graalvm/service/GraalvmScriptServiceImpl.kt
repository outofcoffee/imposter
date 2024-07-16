/*
 * Copyright (c) 2016-2024.
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
package io.gatehill.imposter.scripting.graalvm.service

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.dsl.Dsl
import io.gatehill.imposter.scripting.common.util.JavaScriptUtil
import io.gatehill.imposter.scripting.graalvm.GraalvmScriptingModule
import io.gatehill.imposter.scripting.graalvm.model.objectProxyRequestBuilder
import io.gatehill.imposter.scripting.graalvm.storeproxy.ObjectProxyingStore
import io.gatehill.imposter.service.ScriptRequestBuilder
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.service.ScriptSource
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.util.InjectorUtil
import org.apache.logging.log4j.LogManager
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess


/**
 * Graal implementation of JavaScript scripting engine,
 * with modern JS features enabled.
 *
 * @author Pete Cornish
 */
@PluginInfo("js-graal")
@RequireModules(GraalvmScriptingModule::class)
class GraalvmScriptServiceImpl : ScriptService, Plugin {
    private val engine: Engine

    override val requestBuilder: ScriptRequestBuilder
        get() = objectProxyRequestBuilder

    init {
        // quieten interpreter mode warning until native graal compiler included in module path - see:
        // https://www.graalvm.org/reference-manual/js/RunOnJDK/
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")

        engine = Engine.newBuilder(JS_LANG_ID).build()
    }

    private val enableStoreProxy = EnvVars.getEnv(ENV_IMPOSTER_GRAAL_STORE_PROXY)?.toBoolean() != false
    private var checkedStoreProxy = false

    override fun initScript(script: ScriptSource) {
        checkEnableStoreProxy()
    }

    override fun initInlineScript(scriptId: String, scriptCode: String) {
        checkEnableStoreProxy()
    }

    fun checkEnableStoreProxy() {
        if (!checkedStoreProxy) {
            checkedStoreProxy = true

            if (enableStoreProxy) {
                LOGGER.trace("Graal store proxy enabled")
                val storeFactory = InjectorUtil.getInstance<StoreFactory>()
                storeFactory.storeInterceptors += { ObjectProxyingStore(it) }
            } else {
                LOGGER.trace("Graal store proxy disabled")
            }
        }
    }

    override fun executeScript(
        script: ScriptSource,
        runtimeContext: RuntimeContext,
    ): ReadWriteResponseBehaviour = try {
        LOGGER.trace("Executing script: {}", script)

        val wrapped = JavaScriptUtil.wrapScript(script)

        buildContext().use { context ->
            val bindings = context.getBindings(JS_LANG_ID)
            JavaScriptUtil.transformRuntimeMap(
                runtimeContext,
                addDslPrefix = true,
                addConsoleShim = false
            ).map { (key, value) ->
                bindings.putMember(key, value)
            }

            val result = context.eval(JS_LANG_ID, wrapped.code)
            val dsl = result.`as`(Dsl::class.java)
            return dsl.responseBehaviour
        }
    } catch (e: Exception) {
        throw RuntimeException("Script execution terminated abnormally", e)
    }

    override fun evalInlineScript(
        scriptId: String,
        scriptCode: String,
        runtimeContext: RuntimeContext,
    ): Boolean {
        LOGGER.trace("Executing inline script: {}", scriptId)

        try {
            buildContext().use { context ->
                val bindings = context.getBindings(JS_LANG_ID)
                JavaScriptUtil.transformRuntimeMap(
                    runtimeContext,
                    addDslPrefix = false,
                    addConsoleShim = false
                ).map { (key, value) ->
                    bindings.putMember(key, value)
                }

                val result = context.eval(JS_LANG_ID, scriptCode)
                val resultValue = result.`as`(Any::class.java)
                return resultValue is Boolean && resultValue
            }
        } catch (e: Exception) {
            throw RuntimeException("Inline script evaluation terminated abnormally", e)
        }
    }

    private fun buildContext(): Context = Context.newBuilder(JS_LANG_ID)
        .engine(engine)
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup { _ -> true }
        .build()

    companion object {
        private val LOGGER = LogManager.getLogger(GraalvmScriptServiceImpl::class.java)
        private const val JS_LANG_ID = "js"
        const val ENV_IMPOSTER_GRAAL_STORE_PROXY = "IMPOSTER_GRAAL_STORE_PROXY"
    }
}
