package io.gatehill.imposter.store.impl;

import io.gatehill.imposter.store.model.Store;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryStoreLocatorImpl extends AbstractStoreLocator {
    @Override
    protected Store buildNewStore() {
        return new InMemoryStore();
    }

    private static class InMemoryStore implements Store {
        private final Map<String, Object> store = newHashMap();

        @Override
        public String getTypeDescription() {
            return "inmem";
        }

        @Override
        public void save(String key, Object value) {
            store.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T load(String key) {
            return (T) store.get(key);
        }
    }
}
