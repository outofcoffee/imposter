package io.gatehill.imposter.store.model;

import io.gatehill.imposter.store.util.StoreUtil;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreHolder {
    private final StoreFactory storeFactory;
    private final String requestId;

    public StoreHolder(StoreFactory storeFactory, String requestId) {
        this.storeFactory = storeFactory;
        this.requestId = requestId;
    }

    public Store open(String storeName) {
        if (StoreUtil.isRequestScopedStore(storeName)) {
            storeName = StoreUtil.buildRequestStoreName(requestId);
            return storeFactory.getStoreByName(storeName, true);
        }
        return storeFactory.getStoreByName(storeName, false);
    }
}
