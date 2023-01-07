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
package io.gatehill.imposter.config.resolver

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Verifies converting wiremock mappings to Imposter config.
 *
 * @author Pete Cornish
 */
class WiremockConfigResolverTest {
    private val configResolver = WiremockConfigResolver()

    @Test
    fun `can handle dir containing wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-simple")!!.toURI())
        assertTrue("Should handle wiremock mappings dir", configResolver.handles(mappingsDir.absolutePath))

        val configDir = File(WiremockConfigResolverTest::class.java.getResource("/config")!!.toURI())
        assertFalse("Should not handle imposter config dir", configResolver.handles(configDir.absolutePath))
    }

    @Test
    fun `can convert simple wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-simple")!!.toURI())

        val configDir = configResolver.resolve(mappingsDir.absolutePath)
        assertTrue("Config dir should exist", configDir.exists())
        assertThat("Config dir should differ from source dir", configDir, not(equalTo(mappingsDir)))

        val files = configDir.listFiles()?.map { it.name }
        assertEquals(2, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("response.json"))
    }

    @Test
    fun `can convert templated wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-templated")!!.toURI())

        val configDir = configResolver.resolve(mappingsDir.absolutePath)
        assertTrue("Config dir should exist", configDir.exists())
        assertThat("Config dir should differ from source dir", configDir, not(equalTo(mappingsDir)))

        val files = configDir.listFiles()?.map { it.name }
        assertEquals(2, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("response.xml"))

        val responseFile = File(configDir, "response.xml").readText()
        assertThat(responseFile, not(containsString("{{")))
        assertThat(responseFile, containsString("\${context.request.body://getPetByIdRequest/id}"))
        assertThat(responseFile, containsString("\${random.alphabetic(length=5,uppercase=true)}"))
    }
}
