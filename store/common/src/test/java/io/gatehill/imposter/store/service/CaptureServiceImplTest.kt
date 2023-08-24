/*
 * Copyright (c) 2023.
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

package io.gatehill.imposter.store.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.plugin.config.capture.CaptureConfig
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.store.core.Store
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.util.DateTimeUtil
import io.gatehill.imposter.util.PlaceholderUtil
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDate

class CaptureServiceImplTest {
    @Test
    fun `capture value using expression`() {
        val store = mock<Store>()
        val storeFactory = mock<StoreFactory> {
            on { getStoreByName(any(), any()) } doReturn store
        }
        val service = CaptureServiceImpl(
            storeFactory = storeFactory,
            engineLifecycle = EngineLifecycleHooks(),
        )

        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "test-id"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }

        service.captureItem(
            captureConfigKey = "correlationId",
            itemConfig = ItemCaptureConfig(
                _key = "foo",
                expression = "\${context.request.headers.Correlation-ID}",
                _store = "test",
            ),
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        verify(store).save(eq("foo"), eq("test-id"), any())
    }

    @Test
    fun `generate key using expression`() {
        val store = mock<Store>()
        val storeFactory = mock<StoreFactory> {
            on { getStoreByName(any(), any()) } doReturn store
        }
        val service = CaptureServiceImpl(
            storeFactory = storeFactory,
            engineLifecycle = EngineLifecycleHooks(),
        )

        val request = mock<HttpRequest>()
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }

        service.captureItem(
            captureConfigKey = "correlationId",
            itemConfig = ItemCaptureConfig(
                _key = "key_\${datetime.now.iso8601_date}",
                constValue = "bar",
                _store = "test",
            ),
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        // check key name calculated correctly
        val keyName = "key_${DateTimeUtil.DATE_FORMATTER.format(LocalDate.now())}"
        verify(store).save(eq(keyName), eq("bar"), any())
    }

    @Test
    fun `generate store name using expression`() {
        val store = mock<Store>()
        val storeFactory = mock<StoreFactory> {
            on { getStoreByName(any(), any()) } doReturn store
        }
        val service = CaptureServiceImpl(
            storeFactory = storeFactory,
            engineLifecycle = EngineLifecycleHooks(),
        )

        val request = mock<HttpRequest>()
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }

        service.captureItem(
            captureConfigKey = "correlationId",
            itemConfig = ItemCaptureConfig(
                _key = CaptureConfig(
                    constValue = "foo"
                ),
                constValue = "bar",
                _store = "store_\${datetime.now.iso8601_date}",
            ),
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        verify(store).save(eq("foo"), eq("bar"), any())

        // check store name calculated correctly
        val storeName = "store_${DateTimeUtil.DATE_FORMATTER.format(LocalDate.now())}"
        verify(storeFactory).getStoreByName(eq(storeName), eq(false))
    }
}
