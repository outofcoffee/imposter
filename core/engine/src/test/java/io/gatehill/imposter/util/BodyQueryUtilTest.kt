/*
 * Copyright (c) 2022-2023.
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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.core.IsEqual
import org.jdom2.Document
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.StringReader
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for [BodyQueryUtil].
 */
class BodyQueryUtilTest {
    @Test
    fun `query request body using XPath`() {
        val body = """<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <pets:getPetByIdRequest xmlns:pets="urn:com:example:petstore">
      <pets:id>10</pets:id>
    </pets:getPetByIdRequest>
  </env:Body>
</env:Envelope>"""

        val exchange = mock<HttpExchange> {
            val httpRequest = mock<HttpRequest> {
                on { bodyAsString } doReturn body
            }
            on { this.request } doReturn httpRequest
            on { getOrPut<AtomicReference<Document>>(anyString(), org.mockito.kotlin.any()) } doReturn AtomicReference<Document>()
        }

        val namespaces  = mapOf(
            "env" to "http://www.w3.org/2003/05/soap-envelope",
            "pets" to "urn:com:example:petstore",
        )
        val xPath = "/env:Envelope/env:Body/pets:getPetByIdRequest/pets:id"

        val result = BodyQueryUtil.queryRequestBodyXPath(xPath, namespaces, exchange)
        assertThat(result, equalTo("10"))
    }

    @Test
    fun `query document with non-namespaced XPath`() {
        val body = """<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <getPetByIdRequest>
      <id>10</id>
    </getPetByIdRequest>
  </env:Body>
</env:Envelope>"""

        val document = SAXBuilder().build(StringReader(body))
        val xPath = "//id"
        val result = BodyQueryUtil.selectSingleNode(document, xPath, emptyList())?.value
        assertThat(result, equalTo("10"))
    }

    @Test
    fun `query document attribute XPath`() {
        val body = """<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <pets:getPetByIdRequest xmlns:pets="urn:com:example:petstore" id="10" />
  </env:Body>
</env:Envelope>"""

        val document = SAXBuilder().build(StringReader(body))
        val xPath = "//pets:getPetByIdRequest/@id"
        val result = BodyQueryUtil.getXPathValue(document, xPath,
                listOf(Namespace.getNamespace("pets", "urn:com:example:petstore")))
        assertThat(result, equalTo("10"))
    }

    @Test
    fun normaliseXPathExpression() {
        val normalised = BodyQueryUtil.normaliseXPathExpression("//foo/bar/text()")
        assertThat(normalised, IsEqual.equalTo("//*[local-name()='foo']/*[local-name()='bar']"))
    }

    @Test
    fun normaliseXPathExpressionIgnoreExisting() {
        val normalised = BodyQueryUtil.normaliseXPathExpression("//*[local-name()='foo']/bar")
        assertThat(normalised, IsEqual.equalTo("//*[local-name()='foo']/*[local-name()='bar']"))
    }
}
