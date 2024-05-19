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
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.store.core.PrefixedKeyStore
import io.gatehill.imposter.store.core.Store
import io.gatehill.imposter.store.factory.AbstractStoreFactory.Companion.ENV_VAR_KEY_PREFIX
import io.gatehill.imposter.store.inmem.InMemoryStore
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Common store factory methods. Supports adding a prefix to keys by setting the [ENV_VAR_KEY_PREFIX]
 * environment variable.
 *
 * Ephemeral stores are always backed by an in-memory implementation, regardless of store implementation.
 *
 * @author Pete Cornish
 */
abstract class AbstractStoreFactory (
    private val deferredOperationService: DeferredOperationService,
) : StoreFactory {
    protected val stores: MutableMap<String, Store> = ConcurrentHashMap()
    private val keyPrefix: String?

    override val storeInterceptors: MutableList<(Store) -> Store> = mutableListOf()

    init {
        keyPrefix = getEnv(ENV_VAR_KEY_PREFIX)?.let { "$it." }
    }

    override fun getStoreByName(storeName: String, ephemeral: Boolean): Store {
        var store: Store = stores.getOrPut(storeName) {
            LOGGER.trace("Initialising new store: {}", storeName)
            return@getOrPut if (ephemeral) {
                InMemoryStore(deferredOperationService, storeName, true)
            } else {
                val rawStore = buildNewStore(storeName)
                keyPrefix?.let { PrefixedKeyStore(keyPrefix, rawStore) } ?: rawStore
            }
        }
        LOGGER.trace("Got store: {} (type: {})", storeName, store.typeDescription)
        storeInterceptors.forEach { interceptor -> store = interceptor(store) }
        return store
    }

    override fun clearStore(storeName: String, ephemeral: Boolean) {
        stores.remove(storeName)?.let {
            LOGGER.trace("Cleared store: {}", storeName)
        }
    }

    /**
     * Build a store for the given name. The store must not be ephemeral.
     */
    abstract fun buildNewStore(storeName: String): Store

    companion object {
        private const val ENV_VAR_KEY_PREFIX = "IMPOSTER_STORE_KEY_PREFIX"
        private val LOGGER = LogManager.getLogger(AbstractStoreFactory::class.java)
    }
}
