package io.gatehill.imposter.store.impl;

import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.model.StoreLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractStoreLocator implements StoreLocator {
    private static final Logger LOGGER = LogManager.getLogger(AbstractStoreLocator.class);

    private final Map<String, Store> stores = newHashMap();

    @Override
    public boolean hasStoreWithName(String storeName) {
        return stores.containsKey(storeName);
    }

    @Override
    public Store getStoreByName(String storeName) {
        Store store;
        if (isNull(store = stores.get(storeName))) {
            LOGGER.debug("Initialising new store: {}", storeName);
            store = buildNewStore();
            stores.put(storeName, store);
        }
        LOGGER.trace("Got store: {} (type: {})", storeName, store.getTypeDescription());
        return store;
    }

    protected abstract Store buildNewStore();
}
