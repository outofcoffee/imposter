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

package io.gatehill.imposter.script

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Tests for [ScriptUtil].
 */
class ScriptUtilTest {
    @Test
    fun buildContext() {
        val request = mock<HttpRequest> {
            on { path } doReturn "/foo/bar"
            on { method } doReturn HttpMethod.GET
            on { absoluteUri } doReturn "http://localhost:8080/foo/bar"
            on { pathParams } doReturn mapOf(
                "user" to "alice",
                "hyphen-separated-id" to "1234",
            )
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }
        val context = ScriptUtil.buildContext(httpExchange, emptyMap())
        val pathParams = (context["request"] as ExecutionContext.Request).pathParams
        assertEquals("alice", pathParams["user"])
        assertEquals("1234", pathParams["hyphen-separated-id"])
    }
}
