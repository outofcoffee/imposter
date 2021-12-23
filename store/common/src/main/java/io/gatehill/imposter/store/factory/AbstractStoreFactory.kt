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
package io.gatehill.imposter.store.factory

import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.store.factory.AbstractStoreFactory.Companion.ENV_VAR_KEY_PREFIX
import io.gatehill.imposter.store.inmem.InMemoryStore
import io.gatehill.imposter.store.model.PrefixedKeyStore
import io.gatehill.imposter.store.model.Store
import io.gatehill.imposter.store.model.StoreFactory
import io.gatehill.imposter.store.util.StoreUtil
import org.apache.logging.log4j.LogManager

/**
 * Common store factory methods. Supports adding a prefix to keys by setting the [ENV_VAR_KEY_PREFIX]
 * environment variable.
 *
 * Ephemeral stores are always backed by an in-memory implementation, regardless of store implementation.
 *
 * @author Pete Cornish
 */
abstract class AbstractStoreFactory : StoreFactory {
    private val stores = mutableMapOf<String, Store>()
    private val keyPrefix: String

    init {
        keyPrefix = getEnv(ENV_VAR_KEY_PREFIX)?.let { "$it." } ?: ""
    }

    override fun hasStoreWithName(storeName: String): Boolean {
        return if (StoreUtil.isRequestScopedStore(storeName)) true else stores.containsKey(storeName)
    }

    override fun getStoreByName(storeName: String, isEphemeralStore: Boolean): Store {
        val store: Store = stores.getOrPut(storeName) {
            LOGGER.trace("Initialising new store: {}", storeName)
            return@getOrPut if (isEphemeralStore) {
                InMemoryStore(storeName)
            } else {
                PrefixedKeyStore(keyPrefix, buildNewStore(storeName))
            }
        }
        LOGGER.trace("Got store: {} (type: {})", storeName, store.typeDescription)
        return store
    }

    override fun deleteStoreByName(storeName: String, isEphemeralStore: Boolean) {
        stores.remove(storeName)?.let {
            LOGGER.trace("Deleted store: {}", storeName)
        }
    }

    abstract fun buildNewStore(storeName: String): Store

    companion object {
        private const val ENV_VAR_KEY_PREFIX = "IMPOSTER_STORE_KEY_PREFIX"
        private val LOGGER = LogManager.getLogger(AbstractStoreFactory::class.java)
    }
}