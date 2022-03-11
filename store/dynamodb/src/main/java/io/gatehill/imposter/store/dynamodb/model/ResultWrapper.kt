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
package io.gatehill.imposter.store.dynamodb.model

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.ScanRequest

/**
 * Wraps a scan or query operation and provides access to result items and count.
 */
interface ResultWrapper {
    val items: List<Map<String, AttributeValue>>
    val count: Int

    companion object {
        /**
         * Perform a scan operation on the given table identified by [tableName], with results filtered by [storeName].
         */
        fun usingScan(ddb: AmazonDynamoDB, tableName: String, storeName: String): ResultWrapper {
            val result = ddb.scan(
                ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression("StoreName = :storeName")
                    .withExpressionAttributeValues(mapOf(":storeName" to AttributeValue().withS(storeName)))
            )

            return object : ResultWrapper {
                override val items: List<Map<String, AttributeValue>>
                    get() = result.items

                override val count: Int
                    get() = result.count
            }
        }

        /**
         * Perform a query operation on the given table identified by [tableName], with a key condition restricted by [storeName].
         */
        fun usingQuery(ddb: AmazonDynamoDB, tableName: String, storeName: String): ResultWrapper {
            val result = ddb.query(
                QueryRequest()
                    .withTableName(tableName)
                    .withKeyConditionExpression("StoreName = :storeName")
                    .withExpressionAttributeValues(mapOf(":storeName" to AttributeValue().withS(storeName)))
            )

            return object : ResultWrapper {
                override val items: List<Map<String, AttributeValue>>
                    get() = result.items

                override val count: Int
                    get() = result.count
            }
        }
    }
}
