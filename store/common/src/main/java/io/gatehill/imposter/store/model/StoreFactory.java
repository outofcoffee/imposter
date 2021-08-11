package io.gatehill.imposter.store.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface StoreFactory {
    String REQUEST_SCOPED_STORE_NAME = "request";

    boolean hasStoreWithName(String storeName);

    Store getStoreByName(String storeName, boolean forceInMemory);

    void deleteStoreByName(String storeName);
}
