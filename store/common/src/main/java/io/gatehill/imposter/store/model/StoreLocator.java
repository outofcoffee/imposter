package io.gatehill.imposter.store.model;

import io.gatehill.imposter.store.model.Store;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface StoreLocator {
    Store getStoreByName(String storeName);
}
