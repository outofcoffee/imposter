/*
 * Copyright (c) 2022.
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

package io.gatehill.imposter.plugin.soap.util

import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.vertx.core.buffer.Buffer
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.xpath.XPath

object SoapUtil {
    const val soapContentType = "application/soap+xml"
    private val soap12DraftEnvNamespace: Namespace = Namespace.getNamespace(
        "soap-env",
        "http://www.w3.org/2003/05/soap-envelope"
    )
    val soap12RecEnvNamespace: Namespace = Namespace.getNamespace(
        "soap-env",
        "http://www.w3.org/2001/12/soap-envelope"
    )

    fun buildXPath(expression: String, xPathNamespaces: List<Namespace> = emptyList()): XPath =
        XPath.newInstance(expression).apply {
            xPathNamespaces.forEach { addNamespace(it.prefix, it.uri) }
        }

    fun parseSoapEnvelope(body: Buffer): ParsedSoapMessage {
        val doc = body.bytes.inputStream().use { stream ->
            SAXBuilder().build(stream)
        }
        val envNs = when (doc.rootElement.namespace) {
            soap12DraftEnvNamespace -> soap12DraftEnvNamespace
            soap12RecEnvNamespace -> soap12RecEnvNamespace
            else -> throw IllegalStateException("Root element is not a SOAP envelope - namespace is ${doc.rootElement.namespace}")
        }
        val soapBody = buildXPath("/soap-env:Envelope/soap-env:Body", listOf(envNs))
            .selectSingleNode(doc) as Element?

        return ParsedSoapMessage(soapBody, envNs)
    }

    fun wrapInEnv(body: String, soapNamespace: Namespace): String {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="${soapNamespace.uri}"> 
  <env:Header/>
  <env:Body>
${body.replaceIndent("    ")}
  </env:Body>
</env:Envelope>
""".trim()
    }
}
