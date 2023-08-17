/*
 * Copyright (c) 2023.
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

package io.gatehill.imposter.awslambda.util

import com.google.inject.Module
import io.gatehill.imposter.awslambda.config.Settings
import io.gatehill.imposter.plugin.StaticPluginDiscoveryStrategyImpl
import io.gatehill.imposter.plugin.openapi.OpenApiModule
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl
import io.gatehill.imposter.plugin.rest.RestPluginImpl
import io.gatehill.imposter.plugin.soap.SoapModule
import io.gatehill.imposter.plugin.soap.SoapPluginImpl
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule
import io.gatehill.imposter.scripting.nashorn.service.NashornScriptServiceImpl
import io.gatehill.imposter.store.dynamodb.DynamoDBStoreFactoryImpl
import io.gatehill.imposter.store.dynamodb.DynamoDBStoreModule
import io.gatehill.imposter.store.inmem.InMemoryStoreFactoryImpl
import io.gatehill.imposter.store.inmem.InMemoryStoreModule
import io.gatehill.imposter.util.ClassLoaderUtil

object PluginUtil {
    private val defaultPluginClasses = mapOf(
        "openapi" to OpenApiPluginImpl::class.java,
        "rest" to RestPluginImpl::class.java,
        "soap" to SoapPluginImpl::class.java,
        "js-nashorn" to NashornScriptServiceImpl::class.java,
        "store-inmem" to InMemoryStoreFactoryImpl::class.java,
        "store-dynamodb" to DynamoDBStoreFactoryImpl::class.java,
    )

    private val defaultDependencies = listOf(
        LambdaModule(),
        OpenApiModule(),
        SoapModule(),
        NashornScriptingModule(),
        DynamoDBStoreModule(),
        InMemoryStoreModule(),
    )

    fun buildStaticDiscoveryStrategy(): StaticPluginDiscoveryStrategyImpl {
        val pluginClasses = defaultPluginClasses.toMutableMap()
        Settings.additionalPlugins?.let {
            pluginClasses += it.mapValues { (_, v) -> ClassLoaderUtil.loadClass(v) }
        }

        val dependencies = defaultDependencies.toMutableList<Module>()
        Settings.additionalModules?.let {
            dependencies += it.map { m ->
                val clazz = ClassLoaderUtil.loadClass<Module>(m)
                return@map clazz.getDeclaredConstructor().newInstance()
            }
        }

        return StaticPluginDiscoveryStrategyImpl(pluginClasses, dependencies)
    }
}
