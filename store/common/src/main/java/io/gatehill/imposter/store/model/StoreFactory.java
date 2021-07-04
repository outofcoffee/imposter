package io.gatehill.imposter.store.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface StoreFactory {
    boolean hasStoreWithName(String storeName);

    Store getStoreByName(String storeName);

    void deleteStoreByName(String storeName);
}
