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
package io.gatehill.imposter.store.model

import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.store.util.StoreUtil
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * A delegating store factory that uses the environment variable named by [StoreUtil.envStoreDriver]
 * to determine the [StoreFactory] to use.
 *
 * @author Pete Cornish
 */
class DelegatingStoreFactoryImpl @Inject constructor(
    private val pluginManager: PluginManager,
) : StoreFactory {
    private val logger = LogManager.getLogger(DelegatingStoreFactoryImpl::class.java)

    private val impl: StoreFactory by lazy { loadStoreFactory() }

    private fun loadStoreFactory(): StoreFactory {
        val storeDriver = StoreUtil.activeDriver
        val pluginClass = pluginManager.determinePluginClass(storeDriver)
        logger.trace("Resolved store driver: {} to class: {}", storeDriver, pluginClass)

        try {
            val plugin = pluginManager.getPlugin<Plugin>(pluginClass)
                ?: throw IllegalStateException("Unable to load store driver plugin: $pluginClass")

            return plugin as StoreFactory

        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to load store driver: $storeDriver. Must be an installed plugin implementing ${StoreFactory::class.java.canonicalName}",
                e
            )
        }
    }

    override fun hasStoreWithName(storeName: String): Boolean {
        return impl.hasStoreWithName(storeName)
    }

    override fun getStoreByName(storeName: String, isEphemeralStore: Boolean): Store {
        return impl.getStoreByName(storeName, isEphemeralStore)
    }

    override fun deleteStoreByName(storeName: String, isEphemeralStore: Boolean) {
        impl.deleteStoreByName(storeName, isEphemeralStore)
    }
}
