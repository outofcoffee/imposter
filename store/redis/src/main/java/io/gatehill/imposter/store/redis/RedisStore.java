package io.gatehill.imposter.store.redis;

import io.gatehill.imposter.store.model.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

/**
 * A Redis store implementation. Supports configurable item expiry in seconds,
 * by setting the {@link #ENV_VAR_EXPIRY} environment variable.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class RedisStore implements Store {
    private static final String STORE_TYPE = "redis";
    private static final String ENV_VAR_EXPIRY = "IMPOSTER_STORE_REDIS_EXPIRY";
    private static final Logger LOGGER = LogManager.getLogger(RedisStore.class);

    /**
     * 30 minutes.
     */
    private static final Integer DEFAULT_EXPIRY_SECS = 1800;

    private final String storeName;
    private final RMapCache<String, Object> store;
    private final int expirationSecs;

    public RedisStore(String storeName, RedissonClient redisson) {
        this.storeName = storeName;

        store = redisson.getMapCache(storeName);

        final int expiration = ofNullable(System.getenv(ENV_VAR_EXPIRY))
                .map(Integer::parseInt)
                .orElse(DEFAULT_EXPIRY_SECS);

        if (expiration < 0) {
            expirationSecs = Integer.MAX_VALUE;
            LOGGER.debug("Opened Redis store: {} with no item expiry", storeName);
        } else {
            expirationSecs = expiration;
            LOGGER.debug("Opened Redis store: {} with item expiry: {} seconds", storeName, expirationSecs);
        }
    }

    @Override
    public String getStoreName() {
        return storeName;
    }

    @Override
    public String getTypeDescription() {
        return STORE_TYPE;
    }

    @Override
    public void save(String key, Object value) {
        LOGGER.trace("Saving item with key: {} to store: {}", key, storeName);
        store.put(key, value, expirationSecs, TimeUnit.SECONDS);
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

    @Override
    public int count() {
        final int count = store.size();
        LOGGER.trace("Returning item count {} from store: {}", count, storeName);
        return count;
    }
}
