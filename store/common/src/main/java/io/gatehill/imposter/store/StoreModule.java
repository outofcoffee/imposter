package io.gatehill.imposter.store;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.store.impl.InMemoryStoreLocatorImpl;
import io.gatehill.imposter.store.model.StoreLocator;
import io.gatehill.imposter.store.service.StoreService;
import io.gatehill.imposter.store.service.StoreServiceImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StoreService.class).to(StoreServiceImpl.class).asEagerSingleton();
        bind(StoreLocator.class).to(InMemoryStoreLocatorImpl.class).in(Singleton.class);
    }
}
