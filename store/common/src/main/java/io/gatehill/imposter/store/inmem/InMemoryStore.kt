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

package io.gatehill.imposter.store.inmem

import com.google.common.collect.Maps
import io.gatehill.imposter.store.model.Store
import org.apache.logging.log4j.LogManager

/**
 * An in-memory store implementation. Does not have any support for item expiration,
 * so data must be managed by the caller.
 *
 * @author Pete Cornish
 */
class InMemoryStore(override val storeName: String) : Store {
    private val store: MutableMap<String, Any> = Maps.newConcurrentMap()
    override val typeDescription = STORE_TYPE

    override fun save(key: String, value: Any?) {
        LOGGER.trace("Saving item with key: {} to store: {}", key, storeName)
        value?.let { store[key] = value } ?: store.remove(key)
    }

    override fun <T> load(key: String): T? {
        LOGGER.trace("Loading item with key: {} from store: {}", key, storeName)
        @Suppress("UNCHECKED_CAST")
        return store[key] as T?
    }

    override fun delete(key: String) {
        LOGGER.trace("Deleting item with key: {} from store: {}", key, storeName)
        store.remove(key)
    }

    override fun loadAll(): Map<String, Any> {
        LOGGER.trace("Loading all items in store: {}", storeName)
        return store
    }

    override fun hasItemWithKey(key: String): Boolean {
        LOGGER.trace("Checking for item with key: {} in store: {}", key, storeName)
        return store.containsKey(key)
    }

    override fun count(): Int {
        val count = store.size
        LOGGER.trace("Returning item count {} from store: {}", count, storeName)
        return count
    }

    companion object {
        private const val STORE_TYPE = "inmem"
        private val LOGGER = LogManager.getLogger(InMemoryStore::class.java)
    }
}