package io.gatehill.imposter.store.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreHolder {
    private final StoreLocator storeLocator;

    public StoreHolder(StoreLocator storeLocator) {
        this.storeLocator = storeLocator;
    }

    public Store open(String storeName) {
        return storeLocator.getStoreByName(storeName);
    }
}
