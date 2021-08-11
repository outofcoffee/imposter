package io.gatehill.imposter.store.model;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A delegating {@link Store} wrapper that prepends a string to item keys
 * before persistence and retrieval.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PrefixedKeyStore implements Store {
    private final String keyPrefix;
    private final Store delegate;

    public PrefixedKeyStore(String keyPrefix, Store delegate) {
        this.keyPrefix = keyPrefix;
        this.delegate = delegate;
    }

    @Override
    public String getStoreName() {
        return delegate.getStoreName();
    }

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    @Override
    public String getTypeDescription() {
        return delegate.getTypeDescription();
    }

    @Override
    public void save(String key, Object value) {
        delegate.save(buildKey(key), value);
    }

    @Override
    public <T> T load(String key) {
        return delegate.load(buildKey(key));
    }

    @Override
    public void delete(String key) {
        delegate.delete(buildKey(key));
    }

    @Override
    public Map<String, Object> loadAll() {
        // strip out key prefix
        return delegate.loadAll().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().substring(keyPrefix.length()), Map.Entry::getValue));
    }

    @Override
    public boolean hasItemWithKey(String key) {
        return delegate.hasItemWithKey(buildKey(key));
    }
}
