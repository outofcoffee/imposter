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
package io.gatehill.imposter.server

import com.google.inject.AbstractModule
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.ScriptExecLifecycleHooks
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.util.ClassLoaderUtil
import io.vertx.core.Vertx
import javax.inject.Singleton

/**
 * @author Pete Cornish
 */
class BootstrapModule(
    private val vertx: Vertx,
    private val imposterConfig: ImposterConfig?,
    private val serverFactory: String
) : AbstractModule() {

    override fun configure() {
        bind(Vertx::class.java).toInstance(vertx)
        bind(ImposterConfig::class.java).toInstance(imposterConfig)

        try {
            @Suppress("UNCHECKED_CAST")
            val serverFactoryClass = ClassLoaderUtil.loadClass<ServerFactory>(serverFactory)
            bind(ServerFactory::class.java).to(serverFactoryClass).`in`(Singleton::class.java)

        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Could not load server factory: $serverFactory", e)
        }

        bind(EngineLifecycleHooks::class.java).`in`(Singleton::class.java)
        bind(SecurityLifecycleHooks::class.java).`in`(Singleton::class.java)
        bind(ScriptExecLifecycleHooks::class.java).`in`(Singleton::class.java)
    }
}
