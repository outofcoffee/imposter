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

package io.gatehill.imposter.awslambda.util

import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.util.HttpUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class FormParserUtilTest {
    @Test
    fun `parse form parameters`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq(HttpUtil.CONTENT_TYPE)) } doReturn HttpUtil.CONTENT_TYPE_FORM_URLENCODED
            on { bodyAsString } doReturn "foo=bar&baz=qux%20corge"
        }
        assertEquals("bar", FormParserUtil.getParam(request, "foo"))
        assertEquals("qux corge", FormParserUtil.getParam(request, "baz"))
        assertNull(FormParserUtil.getParam(request, "grault"))
    }

    @Test
    fun `read all form parameters`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq(HttpUtil.CONTENT_TYPE)) } doReturn HttpUtil.CONTENT_TYPE_FORM_URLENCODED
            on { bodyAsString } doReturn "foo=bar&baz=qux%20corge"
        }
        val all = FormParserUtil.getAll(request)
        assertEquals(2, all.size)
        assertEquals("bar", all["foo"])
        assertEquals("qux corge", all["baz"])
    }
}
