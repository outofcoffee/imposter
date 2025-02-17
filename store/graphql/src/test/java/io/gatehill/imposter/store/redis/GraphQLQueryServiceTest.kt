/*
 * Copyright (c) 2016-2024.
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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.graphql.GraphQLQueryService
import io.gatehill.imposter.store.inmem.InMemoryStoreFactoryImpl
import io.gatehill.imposter.util.HttpUtil
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.CompletableFuture

/**
 * Tests for [GraphQLQueryService].
 *
 * @author Pete Cornish
 */
class GraphQLQueryServiceTest {
    private lateinit var service: GraphQLQueryService
    private lateinit var storeFactory: StoreFactory
    private lateinit var httpExchange: HttpExchange
    private lateinit var httpResponse: HttpResponse

    @BeforeEach
    fun before() {
        storeFactory = InMemoryStoreFactoryImpl(mock())
        service = GraphQLQueryService(storeFactory, EngineLifecycleHooks())

        httpResponse = mock {
            on { putHeader(any(), any()) } doAnswer { httpResponse }
        }
        httpExchange = mock {
            on { this.response } doReturn httpResponse
        }
    }

    /**
     * Use GraphQL to query all items in a store.
     */
    @Test
    fun `query all items`() {
        val store = storeFactory.getStoreByName("test", true)
        store.save("foo", "bar")
        store.save("baz", "qux")

        val query = """
            query {
              items(storeName: "test") {
                key
                value
                __typename
              }
            }
        """.trimIndent()
        val body = queryAndReadResponse(query)

        val json = JsonObject(body)
        val graphQlData = json.getJsonObject("data")
        assertNotNull(graphQlData, "GraphQL data property should exist")

        val items = graphQlData.getJsonArray("items")
        assertNotNull(items, "Items should be present in GraphQL response")
        assertEquals(2, items.size())

        val firstItem = items.mapIndexedNotNull { i, _ -> items.getJsonObject(i) }
            .find { it.getString("key") == "foo" }

        assertEquals("foo", firstItem?.getString("key"))
        assertEquals("bar", firstItem?.getString("value"))
        assertEquals("StoreItem", firstItem?.getString("__typename"))
    }

    /**
     * Use a key prefix in an GraphQL query of the store.
     */
    @Test
    fun `query items with prefix`() {
        val store = storeFactory.getStoreByName("test", true)
        store.save("foo", "bar")
        store.save("baz", "qux")

        val query = """
            query {
              items(storeName: "test", keyPrefix: "f") {
                key
                value
                __typename
              }
            }
        """.trimIndent()
        val body = queryAndReadResponse(query)

        val json = JsonObject(body)
        val graphQlData = json.getJsonObject("data")
        assertNotNull(graphQlData, "GraphQL data property should exist")

        val items = graphQlData.getJsonArray("items")
        assertNotNull(items, "Items should be present in GraphQL response")
        assertEquals(1, items.size())

        val firstItem = items.getJsonObject(0)
        assertEquals("foo", firstItem.getString("key"))
        assertEquals("bar", firstItem.getString("value"))
        assertEquals("StoreItem", firstItem.getString("__typename"))
    }

    private fun queryAndReadResponse(query: String): String {
        runBlocking {
            service.execute(query, "{}", httpExchange, CompletableFuture()).join()
        }

        verify(httpExchange).response
        verify(httpResponse).putHeader(eq(HttpUtil.CONTENT_TYPE), eq(HttpUtil.CONTENT_TYPE_JSON))

        val endCaptor = argumentCaptor<String>()
        verify(httpResponse).end(endCaptor.capture())

        val body = endCaptor.firstValue
        assertNotNull("Response body should contain GraphQL response", body)
        return body
    }
}
