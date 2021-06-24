package io.gatehill.imposter.store.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface Store {
    String getTypeDescription();

    void save(String key, Object value);

    <T> T load(String key);
}
