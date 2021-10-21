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

package io.gatehill.imposter.store.factory;

import io.gatehill.imposter.store.inmem.InMemoryStore;
import io.gatehill.imposter.store.model.PrefixedKeyStore;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.model.StoreFactory;
import io.gatehill.imposter.store.util.StoreUtil;
import io.gatehill.imposter.util.EnvVars;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * Common store factory methods. Supports adding a prefix to keys by setting the {@link #ENV_VAR_KEY_PREFIX}
 * environment variable.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractStoreFactory implements StoreFactory {
    private static final String ENV_VAR_KEY_PREFIX = "IMPOSTER_STORE_KEY_PREFIX";
    private static final Logger LOGGER = LogManager.getLogger(AbstractStoreFactory.class);

    private final Map<String, Store> stores = newHashMap();
    private final String keyPrefix;

    public AbstractStoreFactory() {
        this.keyPrefix = ofNullable(EnvVars.getEnv(ENV_VAR_KEY_PREFIX)).map(p -> p + ".").orElse("");
    }

    @Override
    public boolean hasStoreWithName(String storeName) {
        if (StoreUtil.isRequestScopedStore(storeName)) {
            return true;
        }
        return stores.containsKey(storeName);
    }

    @Override
    public Store getStoreByName(String storeName, boolean forceInMemory) {
        Store store;
        if (isNull(store = stores.get(storeName))) {
            LOGGER.trace("Initialising new store: {}", storeName);
            if (forceInMemory) {
                store = new InMemoryStore(storeName);
            } else {
                store = new PrefixedKeyStore(keyPrefix, buildNewStore(storeName));
            }
            stores.put(storeName, store);
        }
        LOGGER.trace("Got store: {} (type: {})", storeName, store.getTypeDescription());
        return store;
    }

    @Override
    public void deleteStoreByName(String storeName) {
        if (nonNull(stores.remove(storeName))) {
            LOGGER.trace("Deleted store: {}", storeName);
        }
    }

    public abstract Store buildNewStore(String storeName);
}
