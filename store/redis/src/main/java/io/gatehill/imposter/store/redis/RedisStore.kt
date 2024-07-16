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
package io.gatehill.imposter.store.redis

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.store.core.AbstractStore
import io.gatehill.imposter.store.redis.RedisStore.Companion.ENV_VAR_EXPIRY
import org.apache.logging.log4j.LogManager
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

/**
 * A Redis store implementation. Supports configurable item expiry in seconds,
 * by setting the [ENV_VAR_EXPIRY] environment variable.
 *
 * @author Pete Cornish
 */
class RedisStore(
    deferredOperationService: DeferredOperationService,
    override val storeName: String,
    redisson: RedissonClient,
) : AbstractStore(deferredOperationService) {
    override val typeDescription = "redis"
    override val isEphemeral = false
    private val store: RMapCache<String, Any>
    private var expirationSecs = 0

    init {
        store = redisson.getMapCache(storeName)
        val expiration = EnvVars.getEnv(ENV_VAR_EXPIRY)?.toInt() ?: DEFAULT_EXPIRY_SECS
        if (expiration < 0) {
            expirationSecs = Int.MAX_VALUE
            LOGGER.debug("Opened Redis store: {} with no item expiry", storeName)
        } else {
            expirationSecs = expiration
            LOGGER.debug("Opened Redis store: {} with item expiry: {} seconds", storeName, expirationSecs)
        }
    }

    override fun saveItem(key: String, value: Any?) {
        LOGGER.trace("Saving item with key: {} to store: {}", key, storeName)
        if (null == value) {
            // can't save a null map value - remove existing if present
            store.remove(key)
        } else {
            store.put(key, value, expirationSecs.toLong(), TimeUnit.SECONDS)
        }
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

    override fun loadAll(): Map<String, Any?> {
        LOGGER.trace("Loading all items in store: {}", storeName)
        return store
    }

    override fun loadByKeyPrefix(keyPrefix: String): Map<String, Any?> {
        LOGGER.trace("Loading items in store: $storeName with key prefix: $keyPrefix")
        val matchingKeys = store.keySet("*$keyPrefix*")
        val items = store.getAll(matchingKeys)
        LOGGER.trace("{} items found in store: $storeName with key prefix: $keyPrefix", items.size)
        return items
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
        private const val ENV_VAR_EXPIRY = "IMPOSTER_STORE_REDIS_EXPIRY"
        private val LOGGER = LogManager.getLogger(RedisStore::class.java)

        /**
         * 30 minutes.
         */
        private const val DEFAULT_EXPIRY_SECS = 1800
    }
}
