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
package io.gatehill.imposter.store.dynamodb

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.store.dynamodb.config.Settings
import io.gatehill.imposter.store.dynamodb.support.Example
import io.gatehill.imposter.util.TestEnvironmentUtil
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.localstack.LocalStackContainer

/**
 * Tests for DynamoDB store implementation.
 *
 * @author Pete Cornish
 */
class DynamoDBStoreFactoryImplTest : AbstractDynamoDBStoreTest() {
    companion object {
        private var dynamo: LocalStackContainer? = null

        @BeforeClass
        @JvmStatic
        fun setUp() {
            // These tests need Docker
            TestEnvironmentUtil.assumeDockerAccessible()

            dynamo = startDynamoDb(mapOf(
                "IMPOSTER_DYNAMODB_TABLE" to "Imposter",
                "IMPOSTER_DYNAMODB_TTL" to "-1",
            ))
            createTable("Imposter")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            try {
                if (dynamo?.isRunning == true) {
                    dynamo!!.stop()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    @Test
    fun testBuildNewStore() {
        val store = factory.buildNewStore("test")
        Assert.assertEquals("dynamodb", store.typeDescription)
    }

    @Test
    fun testSaveLoadSimpleItems() {
        val store = factory.buildNewStore("sli")
        Assert.assertEquals(0, store.count())
        store.save("foo", "bar")
        store.save("baz", 123L)
        store.save("qux", true)
        store.save("corge", null)

        Assert.assertEquals("bar", store.load("foo"))
        Assert.assertEquals(123L, store.load("baz"))
        Assert.assertEquals(true, store.load("qux"))
        Assert.assertNull(store.load("corge"))

        val allItems = store.loadAll()
        Assert.assertEquals(4, allItems.size)
        Assert.assertEquals("bar", allItems["foo"])
        Assert.assertTrue("Item should exist", store.hasItemWithKey("foo"))
        Assert.assertEquals(4, store.count())
    }

    @Test
    fun testSaveLoadMap() {
        val store = factory.buildNewStore("map")
        Assert.assertEquals(0, store.count())
        store.save("grault", mapOf("foo" to "bar"))

        val loadedMap = store.load<Map<String, *>>("grault")
        Assert.assertNotNull(loadedMap)
        Assert.assertTrue("Returned value should be a Map", loadedMap is Map)
        Assert.assertEquals("bar", loadedMap!!["foo"])
    }

    @Test
    fun testSaveLoadComplexItemBinary() {
        EnvVars.populate(EnvVars.getEnv().toMutableMap().apply {
            put("IMPOSTER_DYNAMODB_OBJECT_SERIALISATION", Settings.ObjectSerialisation.BINARY.name) }
        )

        val store = factory.buildNewStore("complex-binary")
        Assert.assertEquals(0, store.count())
        store.save("garply", Example("test"))

        // POJO is deserialised as a Map
        val loadedMap = store.load<Map<String, *>>("garply")
        Assert.assertNotNull(loadedMap)
        Assert.assertTrue("Returned value should be a Map", loadedMap is Map)
        Assert.assertEquals("test", loadedMap!!["name"])
    }

    @Test
    fun testSaveLoadComplexItemMap() {
        EnvVars.populate(EnvVars.getEnv().toMutableMap().apply {
            put("IMPOSTER_DYNAMODB_OBJECT_SERIALISATION", Settings.ObjectSerialisation.MAP.name) }
        )

        val store = factory.buildNewStore("complex-map")
        Assert.assertEquals(0, store.count())
        store.save("garply", Example("test"))

        // POJO is deserialised as a Map
        val loadedMap = store.load<Map<String, *>>("garply")
        Assert.assertNotNull(loadedMap)
        Assert.assertTrue("Returned value should be a Map", loadedMap is Map)
        Assert.assertEquals("test", loadedMap!!["name"])
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
