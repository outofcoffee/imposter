package redis;

import com.google.common.base.Charsets;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.redis.RedisStoreFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RedisStoreFactoryImplTest {
    private RedisStoreFactoryImpl factory;

    private GenericContainer redis;

    @Before
    public void setUp() throws Exception {
        // Testcontainers hangs in CircleCI
        assumeThat(System.getenv("CIRCLECI"), not("true"));

        startRedis();

        final Path configDir = Files.createTempDirectory("imposter");
        writeRedissonConfig(configDir);

        final ImposterConfig imposterConfig = new ImposterConfig();
        imposterConfig.setConfigDirs(new String[]{configDir.toString()});

        factory = new RedisStoreFactoryImpl(imposterConfig);
    }

    private void startRedis() {
        redis = new GenericContainer(DockerImageName.parse("redis:5-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());

        redis.start();
    }

    @After
    public void tearDown() {
        try {
            if (nonNull(redis) && redis.isRunning()) {
                redis.stop();
            }
        } catch (Exception ignored) {
        }
    }

    private void writeRedissonConfig(Path configDir) throws IOException {
        final String redissonConfig = "singleServerConfig:\n  address: \"redis://" + redis.getHost() + ":" + redis.getMappedPort(6379) + "\"";
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
