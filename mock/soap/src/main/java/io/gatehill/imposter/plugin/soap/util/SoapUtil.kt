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

import io.gatehill.imposter.plugin.soap.config.SoapPluginConfig
import io.gatehill.imposter.plugin.soap.model.MessageBodyHolder
import io.gatehill.imposter.plugin.soap.model.ParsedRawBody
import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.gatehill.imposter.util.BodyQueryUtil
import io.vertx.core.buffer.Buffer
import org.jdom2.Document
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import javax.xml.namespace.QName

object SoapUtil {
    const val OPERATION_STYLE_DOCUMENT = "document"
    const val OPERATION_STYLE_RPC = "rpc"
    const val NS_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"
    const val textXmlContentType = "text/xml"
    const val soap11ContentType = textXmlContentType
    const val soap12ContentType = "application/soap+xml"

    val soap11EnvNamespace: Namespace = Namespace.getNamespace(
        "soap-env",
        "http://schemas.xmlsoap.org/soap/envelope/"
    )
    private val soap12DraftEnvNamespace: Namespace = Namespace.getNamespace(
        "soap-env",
        "http://www.w3.org/2001/12/soap-envelope"
    )
    val soap12RecEnvNamespace: Namespace = Namespace.getNamespace(
        "soap-env",
        "http://www.w3.org/2003/05/soap-envelope"
    )

    fun parseBody(config: SoapPluginConfig, body: Buffer): MessageBodyHolder {
        return if (config.envelope) {
            parseSoapEnvelope(body)
        } else {
            parseRawBody(body)
        }
    }

    private fun parseSoapEnvelope(body: Buffer): ParsedSoapMessage {
        val doc = parseDoc(body)
        val envNs = when (doc.rootElement.namespace) {
            soap11EnvNamespace -> soap11EnvNamespace
            soap12DraftEnvNamespace -> soap12DraftEnvNamespace
            soap12RecEnvNamespace -> soap12RecEnvNamespace
            else -> throw IllegalStateException("Root element is not a SOAP envelope - namespace is ${doc.rootElement.namespace}")
        }
        val soapBody = BodyQueryUtil.selectSingleNode(doc, "/soap-env:Envelope/soap-env:Body", listOf(envNs))
        return ParsedSoapMessage(soapBody, envNs)
    }

    private fun parseRawBody(body: Buffer): ParsedRawBody {
        val doc = parseDoc(body)
        return ParsedRawBody(doc.rootElement)
    }

    private fun parseDoc(body: Buffer): Document = body.bytes.inputStream().use { stream ->
        SAXBuilder().build(stream)
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

    /**
     * Returns the local part of an element, if prefixed with a namespace. If `elementName` is unqualified,
     * it is returned.
     */
    fun getLocalPart(elementName: String): String {
        return if (elementName.contains(":")) {
            // qualified
            elementName.split(":")[1]
        } else {
            // unqualified
            elementName
        }
    }

    fun QName.toNamespaceMap(): Map<String, String> =
        mapOf(this.prefix to this.namespaceURI)
}
