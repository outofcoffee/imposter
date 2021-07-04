package io.gatehill.imposter.store.inmem;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.store.model.StoreFactory;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StoreFactory.class).to(InMemoryStoreFactoryImpl.class).in(Singleton.class);
    }
}
