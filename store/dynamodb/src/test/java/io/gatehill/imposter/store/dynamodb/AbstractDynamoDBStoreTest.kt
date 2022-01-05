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

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.store.dynamodb.config.Settings
import org.junit.Before
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files

/**
 * Tests for DynamoDB store implementation.
 *
 * @author Pete Cornish
 */
abstract class AbstractDynamoDBStoreTest {
    protected lateinit var factory: DynamoDBStoreFactoryImpl

    companion object {
        lateinit var ddb: AmazonDynamoDB

        fun startDynamoDb(additionalEnv: Map<String, String> = emptyMap()): LocalStackContainer {
            val dynamo = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.2"))
                .withServices(LocalStackContainer.Service.DYNAMODB)
                .apply { start() }

            val dynamoDbEndpoint = dynamo!!.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()
            EnvVars.populate(
                mapOf(
                    "IMPOSTER_DYNAMODB_ENDPOINT" to dynamoDbEndpoint,
                    "AWS_ACCESS_KEY_ID" to "dummy",
                    "AWS_SECRET_ACCESS_KEY" to "dummy",
                ) + additionalEnv
            )

            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(Settings.dynamoDbApiEndpoint, Settings.dynamoDbSigningRegion)
            ).withCredentials(
                AWSStaticCredentialsProvider(BasicAWSCredentials("dummy", "dummy"))
            ).build()

            return dynamo
        }

        fun createTable(tableName: String) {
            val keySchema = listOf(
                KeySchemaElement("StoreName", KeyType.HASH),
                KeySchemaElement("Key", KeyType.RANGE),
            )
            val attributeDefs = listOf(
                AttributeDefinition("StoreName", ScalarAttributeType.S),
                AttributeDefinition("Key", ScalarAttributeType.S),
            )
            val request = CreateTableRequest(tableName, keySchema)
                .withAttributeDefinitions(attributeDefs)
                .withProvisionedThroughput(
                    ProvisionedThroughput()
                        .withReadCapacityUnits(5L)
                        .withWriteCapacityUnits(6L)
                )

            ddb.createTable(request)
        }
    }

    @Before
    fun before() {
        val configDir = Files.createTempDirectory("imposter")

        val imposterConfig = ImposterConfig()
        imposterConfig.configDirs = arrayOf(configDir.toString())
        factory = DynamoDBStoreFactoryImpl()
    }
}
