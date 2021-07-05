package io.gatehill.imposter.store.inmem;

import io.gatehill.imposter.store.factory.AbstractStoreFactory;
import io.gatehill.imposter.store.model.Store;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryStoreFactoryImpl extends AbstractStoreFactory {
    @Override
    public Store buildNewStore(String storeName) {
        return new InMemoryStore(storeName);
    }
}
