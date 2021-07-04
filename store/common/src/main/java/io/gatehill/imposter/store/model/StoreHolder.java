package io.gatehill.imposter.store.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreHolder {
    private final StoreFactory storeFactory;

    public StoreHolder(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public Store open(String storeName) {
        return storeFactory.getStoreByName(storeName);
    }
}
