package io.gatehill.imposter.store.redis;

import com.google.inject.Inject;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.store.factory.AbstractStoreFactory;
import io.gatehill.imposter.store.model.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RedisStoreFactoryImpl extends AbstractStoreFactory {
    private static final Logger LOGGER = LogManager.getLogger(RedisStoreFactoryImpl.class);

    private final RedissonClient redisson;

    @Inject
    public RedisStoreFactoryImpl(ImposterConfig imposterConfig) {
        final Config config;
        try {
            final File configFile = discoverConfigFile(imposterConfig);
            LOGGER.debug("Loading Redisson configuration from: {}", configFile);
            config = Config.fromYAML(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load Redisson configuration file", e);
        }
        redisson = Redisson.create(config);
    }

    private File discoverConfigFile(ImposterConfig imposterConfig) {
        return Arrays.stream(imposterConfig.getConfigDirs()).map(dir -> {
            final File configFile = new File(dir, "redisson.yaml");
            if (configFile.exists()) {
                return configFile;
            } else {
                return new File(dir, "redisson.yml");
            }
        }).filter(File::exists).findFirst().orElseThrow(() ->
                new IllegalStateException("No Redisson configuration file named 'redisson.yaml' found in configuration directories")
        );
    }

    @Override
    public Store buildNewStore(String storeName) {
        return new RedisStore(storeName, redisson);
    }
}
