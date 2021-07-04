package redis;

import com.google.common.base.Charsets;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.redis.RedisStoreFactoryImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RedisStoreFactoryImplTest {
    private RedisStoreFactoryImpl factory;

    @Rule
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);

    @Before
    public void setUp() throws Exception {
        final Path configDir = Files.createTempDirectory("imposter");
        writeRedissonConfig(configDir);

        final ImposterConfig imposterConfig = new ImposterConfig();
        imposterConfig.setConfigDirs(new String[]{configDir.toString()});

        factory = new RedisStoreFactoryImpl(imposterConfig);
    }

    private void writeRedissonConfig(Path configDir) throws IOException {
        final String redissonConfig = "singleServerConfig:\n  address: \"redis://" + redis.getHost() + ":" + redis.getFirstMappedPort() + "\"";
        final File redissonConfigFile = new File(configDir.toFile(), "redisson.yaml");
        com.google.common.io.Files.write(redissonConfig, redissonConfigFile, Charsets.UTF_8);
    }

    @Test
    public void testBuildNewStore() {
        final Store store = factory.buildNewStore("test");
        assertEquals("redis", store.getTypeDescription());
    }

    @Test
    public void testSaveLoadItem() {
        final Store store = factory.buildNewStore("sli");

        store.save("foo", "bar");
        assertEquals("bar", store.load("foo"));

        final Map<String, Object> allItems = store.loadAll();
        assertEquals(1, allItems.size());
        assertEquals("bar", allItems.get("foo"));
        assertTrue("Item should exist", store.hasItemWithKey("foo"));
    }

    @Test
    public void testDeleteItem() {
        final Store store = factory.buildNewStore("di");

        assertFalse("Item should not exist", store.hasItemWithKey("foo"));

        store.save("foo", "bar");
        assertTrue("Item should exist", store.hasItemWithKey("foo"));

        store.delete("foo");
        assertFalse("Item should not exist", store.hasItemWithKey("foo"));
    }
}
