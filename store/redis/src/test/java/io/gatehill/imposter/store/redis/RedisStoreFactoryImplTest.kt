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
package io.gatehill.imposter.store.redis

import com.google.common.base.Charsets
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.util.TestEnvironmentUtil
import io.vertx.core.Vertx
import org.apache.commons.io.FileUtils
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.GenericKontainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for Redis store implementation.
 *
 * @author Pete Cornish
 */
class RedisStoreFactoryImplTest {
    private lateinit var factory: RedisStoreFactoryImpl

    companion object {
        private var redis: GenericContainer<*>? = null
        private var imposterConfig: ImposterConfig? = null

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUp() {
            // These tests need Docker
            TestEnvironmentUtil.assumeDockerAccessible()

            startRedis()

            val configDir = Files.createTempDirectory("imposter")
            writeRedissonConfig(configDir)

            imposterConfig = ImposterConfig().apply {
                configDirs = arrayOf(configDir.toString())
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            try {
                if (redis?.isRunning == true) {
                    redis!!.stop()
                }
            } catch (ignored: Exception) {
            }
        }

        private fun startRedis() {
            redis = GenericKontainer(DockerImageName.parse("redis:5-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort())
                .apply { start() }
        }

        @Throws(IOException::class)
        private fun writeRedissonConfig(configDir: Path) {
            val redissonConfig = """
singleServerConfig:
  address: "redis://${redis!!.host}:${redis!!.getMappedPort(6379)}"
"""

            val redissonConfigFile = File(configDir.toFile(), "redisson.yaml")
            FileUtils.write(redissonConfigFile, redissonConfig, Charsets.UTF_8)
        }
    }

    @Before
    fun before() {
        factory = RedisStoreFactoryImpl(
            DeferredOperationService(Vertx.vertx()),
            imposterConfig!!
        )
    }

    @Test
    fun testBuildNewStore() {
        val store = factory.buildNewStore("test")
        Assert.assertEquals("redis", store.typeDescription)
    }

    @Test
    fun testSaveLoadItem() {
        val store = factory.buildNewStore("sli")
        Assert.assertEquals(0, store.count())
        store.save("foo", "bar")
        Assert.assertEquals("bar", store.load("foo"))
        val allItems = store.loadAll()
        Assert.assertEquals(1, allItems.size)
        Assert.assertEquals("bar", allItems["foo"])
        Assert.assertTrue("Item should exist", store.hasItemWithKey("foo"))
        Assert.assertEquals(1, store.count())
    }

    @Test
    fun testDeleteItem() {
        val store = factory.buildNewStore("di")
        Assert.assertFalse("Item should not exist", store.hasItemWithKey("baz"))
        store.save("baz", "qux")
        Assert.assertTrue("Item should exist", store.hasItemWithKey("baz"))
        store.delete("baz")
        Assert.assertFalse("Item should not exist", store.hasItemWithKey("baz"))
    }

    @Test
    fun testClearStore() {
        val store = factory.buildNewStore("ds")
        store.save("baz", "qux")
        factory.clearStore("ds", false)
        Assert.assertEquals("Store should be empty", 0, factory.getStoreByName("ds", false).count())
    }
}
