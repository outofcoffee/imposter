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
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.store.dynamodb.support.DynamoDBStoreTestHelper
import io.gatehill.imposter.store.factory.AbstractStoreFactory
import io.gatehill.imposter.util.TestEnvironmentUtil
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import java.nio.file.Files

/**
 * Tests for DynamoDB TTL logic.
 *
 * @author Pete Cornish
 */
class DynamoDBStoreTtlTest {
    private lateinit var factory: AbstractStoreFactory

    companion object {
        private const val ttlSeconds = 300
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
                    "IMPOSTER_DYNAMODB_TABLE" to "TtlTest",
                    "IMPOSTER_DYNAMODB_TTL" to ttlSeconds.toString(),
                )
            )
            helper.createTable("TtlTest")
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

    @Before
    fun before() {
        val configDir = Files.createTempDirectory("imposter")

        val imposterConfig = ImposterConfig()
        imposterConfig.configDirs = arrayOf(configDir.toString())
        factory = DynamoDBStoreFactoryImpl(DeferredOperationService())
    }

    @Test
    fun testSaveWithTtl() {
        val store = factory.buildNewStore("ttltest")

        val persistenceTime = System.currentTimeMillis() / 1000
        store.save("foo", "bar")

        val item = helper.ddb.getItem(
            GetItemRequest().withTableName("TtlTest").withKey(
                mapOf(
                    "StoreName" to AttributeValue().withS("ttltest"),
                    "Key" to AttributeValue().withS("foo")
                )
            )
        )

        assertNotNull("Item should exist", item?.item)

        val ttlAttribute = item.item["ttl"]
        assertNotNull("TTL attribute should exist", ttlAttribute)

        val ttlValue = ttlAttribute!!.n
        assertNotNull("TTL should be set to number", ttlValue)
        assertTrue("TTL should be persistence time + configured value", ttlValue.toLong() >= (persistenceTime + ttlSeconds))
    }
}
