package io.gatehill.imposter.store.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.store.model.StoreFactory;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RedisStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StoreFactory.class).to(RedisStoreFactoryImpl.class).in(Singleton.class);
    }
}
