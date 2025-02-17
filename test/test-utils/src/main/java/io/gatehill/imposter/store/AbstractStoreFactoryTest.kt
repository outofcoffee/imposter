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
package io.gatehill.imposter.store

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.store.factory.AbstractStoreFactory
import io.gatehill.imposter.store.support.Example
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Common tests for store factories.
 *
 * @author Pete Cornish
 */
abstract class AbstractStoreFactoryTest {
    protected lateinit var factory: AbstractStoreFactory
    protected abstract val typeDescription: String

    @BeforeEach
    fun before() {
        val configDir = Files.createTempDirectory("imposter")

        val imposterConfig = ImposterConfig()
        imposterConfig.configDirs = arrayOf(configDir.toString())
        factory = buildFactory()
    }

    abstract fun buildFactory(): AbstractStoreFactory

    @Test
    fun testBuildNewStore() {
        val store = factory.buildNewStore("test")
        Assertions.assertEquals(typeDescription, store.typeDescription)
    }

    @Test
    open fun testSaveLoadSimpleItems() {
        factory.clearStore("sli", false)
        val store = factory.buildNewStore("sli")

        Assertions.assertEquals(0, store.count())
        store.save("foo", "bar")
        store.save("baz", 123L)
        store.save("qux", true)
        store.save("corge", null)

        Assertions.assertEquals("bar", store.load("foo"))
        Assertions.assertEquals(123L, store.load("baz"))
        Assertions.assertEquals(true, store.load("qux"))
        Assertions.assertNull(store.load("corge"))

        val allItems = store.loadAll()
        Assertions.assertEquals("bar", allItems["foo"])
        Assertions.assertEquals(123L, allItems["baz"])
        Assertions.assertEquals(true, allItems["qux"])
        Assertions.assertNull(allItems["corge"])
        Assertions.assertTrue(store.hasItemWithKey("foo"), "Item should exist")
    }

    @Test
    fun testLoadByKeyPrefix() {
        val store = factory.buildNewStore("kp")
        Assertions.assertEquals(0, store.count())
        store.save("foo_one", "bar")
        store.save("foo_two", "baz")

        val items = store.loadByKeyPrefix("foo_")
        Assertions.assertEquals("bar", items["foo_one"])
        Assertions.assertEquals("baz", items["foo_two"])
    }

    @Test
    fun testSaveLoadMap() {
        val store = factory.buildNewStore("map")
        Assertions.assertEquals(0, store.count())
        store.save("grault", mapOf("foo" to "bar"))

        val loadedMap = store.load<Map<String, *>>("grault")
        Assertions.assertNotNull(loadedMap)
        Assertions.assertTrue(loadedMap is Map, "Returned value should be a Map")
        Assertions.assertEquals("bar", loadedMap!!["foo"])
    }

    @Test
    open fun testSaveLoadComplexItemBinary() {
        val store = factory.buildNewStore("complex-binary")
        Assertions.assertEquals(0, store.count())
        store.save("garply", Example("test"))

        // POJO is deserialised as a Map
        val loaded = store.load<Example>("garply")
        Assertions.assertNotNull(loaded)
        Assertions.assertTrue(loaded is Example, "Returned value should be a ${Example::class.qualifiedName}")
        Assertions.assertEquals("test", loaded!!.name)
    }

    @Test
    fun testDeleteItem() {
        val store = factory.buildNewStore("di")
        Assertions.assertFalse(store.hasItemWithKey("baz"), "Item should not exist")
        store.save("baz", "qux")
        Assertions.assertTrue(store.hasItemWithKey("baz"), "Item should exist")
        store.delete("baz")
        Assertions.assertFalse(store.hasItemWithKey("baz"), "Item should not exist")
    }

    @Test
    fun testClearStore() {
        val store = factory.buildNewStore("ds")
        store.save("baz", "qux")
        factory.clearStore("ds", false)
        Assertions.assertEquals(0, factory.getStoreByName("ds", false).count(), "Store should be empty")
    }
}
