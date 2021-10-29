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

package io.gatehill.imposter.store.redis;

import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.util.EnvVars;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

/**
 * A Redis store implementation. Supports configurable item expiry in seconds,
 * by setting the {@link #ENV_VAR_EXPIRY} environment variable.
 *
 * @author Pete Cornish
 */
class RedisStore implements Store {
    private static final String STORE_TYPE = "redis";
    private static final String ENV_VAR_EXPIRY = "IMPOSTER_STORE_REDIS_EXPIRY";
    private static final Logger LOGGER = LogManager.getLogger(RedisStore.class);

    /**
     * 30 minutes.
     */
    private static final Integer DEFAULT_EXPIRY_SECS = 1800;

    private final String storeName;
    private final RMapCache<String, Object> store;
    private final int expirationSecs;

    public RedisStore(String storeName, RedissonClient redisson) {
        this.storeName = storeName;

        store = redisson.getMapCache(storeName);

        final int expiration = ofNullable(EnvVars.getEnv(ENV_VAR_EXPIRY))
                .map(Integer::parseInt)
                .orElse(DEFAULT_EXPIRY_SECS);

        if (expiration < 0) {
            expirationSecs = Integer.MAX_VALUE;
            LOGGER.debug("Opened Redis store: {} with no item expiry", storeName);
        } else {
            expirationSecs = expiration;
            LOGGER.debug("Opened Redis store: {} with item expiry: {} seconds", storeName, expirationSecs);
        }
    }

    @Override
    public String getStoreName() {
        return storeName;
    }

    @Override
    public String getTypeDescription() {
        return STORE_TYPE;
    }

    @Override
    public void save(String key, Object value) {
        LOGGER.trace("Saving item with key: {} to store: {}", key, storeName);
        store.put(key, value, expirationSecs, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T load(String key) {
        LOGGER.trace("Loading item with key: {} from store: {}", key, storeName);
        return (T) store.get(key);
    }

    @Override
    public void delete(String key) {
        LOGGER.trace("Deleting item with key: {} from store: {}", key, storeName);
        store.remove(key);
    }

    @Override
    public Map<String, Object> loadAll() {
        LOGGER.trace("Loading all items in store: {}", storeName);
        return store;
    }

    @Override
    public boolean hasItemWithKey(String key) {
        LOGGER.trace("Checking for item with key: {} in store: {}", key, storeName);
        return store.containsKey(key);
    }

    @Override
    public int count() {
        final int count = store.size();
        LOGGER.trace("Returning item count {} from store: {}", count, storeName);
        return count;
    }
}
