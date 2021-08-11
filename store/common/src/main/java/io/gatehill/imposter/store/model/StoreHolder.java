package io.gatehill.imposter.store.model;

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
        if (StoreFactory.REQUEST_SCOPED_STORE_NAME.equals(storeName)) {
            storeName = "request_" + requestId;
            return storeFactory.getStoreByName(storeName, true);
        }
        return storeFactory.getStoreByName(storeName, false);
    }
}
