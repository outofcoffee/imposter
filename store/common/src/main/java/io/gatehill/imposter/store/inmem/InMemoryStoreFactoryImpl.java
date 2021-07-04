package io.gatehill.imposter.store.inmem;

import io.gatehill.imposter.store.factory.AbstractStoreFactory;
import io.gatehill.imposter.store.model.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryStoreFactoryImpl extends AbstractStoreFactory {
    @Override
    public Store buildNewStore(String storeName) {
        return new InMemoryStore(storeName);
    }

    private static class InMemoryStore implements Store {
        private static final String STORE_TYPE = "inmem";
        private static final Logger LOGGER = LogManager.getLogger(InMemoryStore.class);

        private final String storeName;
        private final Map<String, Object> store = newHashMap();

        public InMemoryStore(String storeName) {
            this.storeName = storeName;
        }

        @Override
        public String getTypeDescription() {
            return STORE_TYPE;
        }

        @Override
        public void save(String key, Object value) {
            LOGGER.trace("Saving item with key: {} to store: {}", key, storeName);
            store.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T load(String key) {
            LOGGER.trace("Loading item with key: {} from store: {}", key, storeName);
            return (T) store.get(key);
        }

        @Override
        public void delete(String key) {
            LOGGER.trace("Deleting item with key: {} from store: {}", key, storeName);
            store.remove(key);
        }

        @Override
        public Map<String, Object> loadAll() {
            LOGGER.trace("Loading all items in store: {}", storeName);
            return store;
        }

        @Override
        public boolean hasItemWithKey(String key) {
            LOGGER.trace("Checking for item with key: {} in store: {}", key, storeName);
            return store.containsKey(key);
        }
    }
}
