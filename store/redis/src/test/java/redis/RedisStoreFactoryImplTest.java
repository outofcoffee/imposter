/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package redis;

import com.google.common.base.Charsets;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.redis.RedisStoreFactoryImpl;
import io.gatehill.imposter.util.TestEnvironmentUtil;
import org.apache.commons.io.FileUtils;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RedisStoreFactoryImplTest {
    private RedisStoreFactoryImpl factory;

    private GenericContainer redis;

    @Before
    public void setUp() throws Exception {
        // Testcontainers hangs in CircleCI
        TestEnvironmentUtil.assumeNotInCircleCi();

        // These tests need Docker
        TestEnvironmentUtil.assumeDockerAccessible();

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
        FileUtils.write(redissonConfigFile, redissonConfig, Charsets.UTF_8);
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
        assertEquals(1, store.count());
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
