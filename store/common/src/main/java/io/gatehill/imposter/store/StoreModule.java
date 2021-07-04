package io.gatehill.imposter.store;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.gatehill.imposter.store.inmem.InMemoryStoreModule;
import io.gatehill.imposter.store.service.StoreService;
import io.gatehill.imposter.store.service.StoreServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreModule extends AbstractModule {
    private static final String DEFAULT_STORE_MODULE = InMemoryStoreModule.class.getCanonicalName();
    private static final Logger LOGGER = LogManager.getLogger(StoreModule.class);

    @Override
    protected void configure() {
        bind(StoreService.class).to(StoreServiceImpl.class).asEagerSingleton();
        install(discoverStoreModule());
    }

    @SuppressWarnings("unchecked")
    private Module discoverStoreModule() {
        final String storeModule = ofNullable(System.getenv("IMPOSTER_STORE_MODULE")).orElse(DEFAULT_STORE_MODULE);
        LOGGER.debug("Loading store module: {}", storeModule);
        try {
            final Class<? extends Module> moduleClass = (Class<? extends Module>) Class.forName(storeModule);
            return moduleClass.newInstance();

        } catch (Exception e) {
            throw new RuntimeException("Unable to load store module: " + storeModule +
                    ". Must be a fully qualified class implementing " + Module.class.getCanonicalName() +
                    " with a no-arg constructor.", e);
        }
    }
}
