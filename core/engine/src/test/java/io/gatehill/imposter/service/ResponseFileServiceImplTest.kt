/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

/**
 * Tests for [ResponseFileServiceImpl].
 */
class ResponseFileServiceImplTest {
    @Test
    fun `should serve file contents`() {
        val responseService = mock<ResponseService> {
            on { writeResponseData(any(), any(), any(), any(), any(), any()) } doAnswer {
                val buffer = it.arguments[3] as Buffer
                assertEquals("Hello, world!", buffer.toString())
            }
        }

        val fileSystem = mock<FileSystem> {
            on { readFileBlocking(any()) } doAnswer {
                val path = it.arguments[0] as String
                Buffer.buffer(File(path).readBytes())
            }
        }
        val vertx = mock<Vertx> {
            on { fileSystem() } doReturn fileSystem
        }
        val service = ResponseFileServiceImpl(responseService, vertx)

        val pluginConfig = PluginConfigImpl().apply {
            dir = File(ResponseFileServiceImplTest::class.java.getResource("/response-file.txt")!!.toURI()).parentFile
        }
        val resourceConfig = RestResourceConfig()
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { setStatusCode(any()) } doReturn mock
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        val responseBehaviour = ReadWriteResponseBehaviourImpl().apply {
            responseFile = "response-file.txt"
        }

        service.serveResponseFile(pluginConfig, resourceConfig, httpExchange, responseBehaviour)

        verify(responseService).writeResponseData(
            eq(resourceConfig),
            eq(httpExchange),
            any(),
            any(),
            eq(false),
            eq(false)
        )
    }

    @Test
    fun `should load file as JSON array`() {
        val service = ResponseFileServiceImpl(mock(), mock())

        val jsonFile = File(ResponseFileServiceImplTest::class.java.getResource("/test-array.json")!!.toURI())
        val pluginConfig = PluginConfigImpl().apply {
            dir = jsonFile.parentFile
        }
        val responseBehaviour = ReadWriteResponseBehaviourImpl().apply {
            responseFile = "test-array.json"
        }
        val result = service.loadResponseAsJsonArray(pluginConfig, responseBehaviour)

        assertEquals(1, result.size())
    }
}
