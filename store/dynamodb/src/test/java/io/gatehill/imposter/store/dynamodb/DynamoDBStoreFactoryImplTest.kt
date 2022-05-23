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

import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.regions.Regions
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.store.AbstractStoreFactoryTest
import io.gatehill.imposter.store.dynamodb.config.Settings
import io.gatehill.imposter.store.dynamodb.support.DynamoDBStoreTestHelper
import io.gatehill.imposter.store.support.Example
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
class DynamoDBStoreFactoryImplTest : AbstractStoreFactoryTest() {
    override val typeDescription = "dynamodb"

    companion object {
        private val helper = DynamoDBStoreTestHelper()
        private var dynamo: LocalStackContainer? = null

        @BeforeClass
        @JvmStatic
        fun setUp() {
            // These tests need Docker
            TestEnvironmentUtil.assumeDockerAccessible()

            System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.name)

            dynamo = helper.startDynamoDb(
                mapOf(
                    "IMPOSTER_DYNAMODB_TABLE" to "Imposter",
                    "IMPOSTER_DYNAMODB_TTL" to "-1",
                )
            )
            helper.createTable("Imposter")
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

    override fun buildFactory() = DynamoDBStoreFactoryImpl(DeferredOperationService())

    /**
     * @see testSaveLoadComplexItemMap
     */
    @Test
    override fun testSaveLoadComplexItemBinary() {
        EnvVars.populate(
            EnvVars.getEnv() + mapOf("IMPOSTER_DYNAMODB_OBJECT_SERIALISATION" to Settings.ObjectSerialisation.BINARY.name)
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

    /**
     * @see testSaveLoadComplexItemBinary
     */
    @Test
    fun testSaveLoadComplexItemMap() {
        EnvVars.populate(
            EnvVars.getEnv() + mapOf("IMPOSTER_DYNAMODB_OBJECT_SERIALISATION" to Settings.ObjectSerialisation.MAP.name)
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
}
