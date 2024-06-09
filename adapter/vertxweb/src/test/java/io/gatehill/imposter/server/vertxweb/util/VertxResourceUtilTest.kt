/*
 * Copyright (c) 2024.
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

package io.gatehill.imposter.server.vertxweb.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for [VertxResourceUtil].
 *
 * @author Pete Cornish
 */
class VertxResourceUtilTest {
    @Test
    fun `should convert path to Vertx format`() {
        val normalisedParams = mutableMapOf<String, String>()
        val result = VertxResourceUtil.normalisePath(normalisedParams, "/{pathParam}/notParam")

        assertEquals(0, normalisedParams.size)
        assertEquals("/:pathParam/notParam", result)
    }

    @Test
    fun `should handle param and plain string`() {
        val normalisedParams = mutableMapOf<String, String>()
        val result = VertxResourceUtil.normalisePath(normalisedParams, "/{firstParam}.notParam")

        assertEquals(0, normalisedParams.size)
        assertEquals("/:firstParam.notParam", result)
    }

    @Test
    fun `should normalise path`() {
        val normalisedParams = mutableMapOf<String, String>()
        val result = VertxResourceUtil.normalisePath(normalisedParams, "/{firstParam}/{second-param}/notParam")

        assertEquals(1, normalisedParams.size)
        assertFalse(normalisedParams.containsKey("firstParam"))

        val secondParam = normalisedParams.entries.first()
        assertNotNull(secondParam)
        assertEquals("second-param", secondParam.value)
        assertNotEquals("second-param", secondParam.key)
        assertEquals("/:firstParam/:${secondParam.key}/notParam", result)
    }

    @Test
    fun `should get normalised param name`() {
        val normalisedParams = mapOf(
            "abcdef1234567890" to "kebab-param"
        )
        assertEquals("abcdef1234567890", VertxResourceUtil.getNormalisedParamName(normalisedParams, "kebab-param"))
        assertEquals("normalParam", VertxResourceUtil.getNormalisedParamName(normalisedParams, "normalParam"))
    }

    @Test
    fun `should denormalise params`() {
        val normalisedParams = mapOf(
            "abcdef1234567890" to "kebab-param"
        )
        val vertxParams = mapOf(
            "abcdef1234567890" to "param value",
            "content-type" to "application/json"
        )
        val result = VertxResourceUtil.denormaliseParams(normalisedParams, vertxParams)
        assertEquals("param value", result["kebab-param"])
        assertEquals("application/json", result["content-type"])
    }
}
