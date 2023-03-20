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
package io.gatehill.imposter.util

import io.gatehill.imposter.util.HttpUtil.readAcceptedContentTypes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for [HttpUtil].
 *
 * @author Pete Cornish
 */
class HttpUtilTest {
    @Test
    fun readAcceptedContentTypes() {
        // note: provided out of order, but should be sorted by 'q' weight
        val acceptHeader = "text/html; q=1.0, text/*; q=0.8, image/jpeg; q=0.5, image/gif; q=0.7, image/*; q=0.4, */*; q=0.1"
        val actual = readAcceptedContentTypes(acceptHeader)
        assertNotNull(actual)
        assertEquals(6, actual.size.toLong())

        // check order
        assertEquals("text/html", actual[0])
        assertEquals("text/*", actual[1])
        assertEquals("image/gif", actual[2])
        assertEquals("image/jpeg", actual[3])
        assertEquals("image/*", actual[4])
        assertEquals("*/*", actual[5])
    }

    @Test
    fun joinPaths() {
        assertThat(HttpUtil.joinPaths("/foo", "/bar"), equalTo("/foo/bar"))
        assertThat(HttpUtil.joinPaths("/foo", "bar"), equalTo("/foo/bar"))
        assertThat(HttpUtil.joinPaths("/foo/", "/bar"), equalTo("/foo/bar"))
        assertThat(HttpUtil.joinPaths("/foo/", "/bar/"), equalTo("/foo/bar/"))
        assertThat(HttpUtil.joinPaths("", "/bar"), equalTo("/bar"))
        assertThat(HttpUtil.joinPaths("/foo", ""), equalTo("/foo"))
        assertThat(HttpUtil.joinPaths(null, "/bar"), equalTo("/bar"))
        assertThat(HttpUtil.joinPaths("/foo", null), equalTo("/foo"))
        assertThat(HttpUtil.joinPaths(null, null), nullValue())
    }
}
