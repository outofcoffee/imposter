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

package io.gatehill.imposter.plugin.soap.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument
import javax.xml.namespace.QName

/**
 * Generates schemas for elements.
 */
object SchemaGenerator {
    private val logger: Logger = LogManager.getLogger(SchemaGenerator::class.java)

    fun createSinglePartSchema(elementName: String, partType: QName): SchemaDocument {
        val namespaces = mutableMapOf<String, String>()
        if (partType.namespaceURI?.isNotBlank() == true) {
            namespaces[partType.prefix] = partType.namespaceURI
        }
        if (!namespaces.containsKey("xs")) {
            namespaces["xs"] ="http://www.w3.org/2001/XMLSchema"
        }

        val namespacesXml = namespaces.entries.joinToString(separator = "\n") { (prefix, nsUri) ->
            """xmlns:${prefix}="${nsUri}""""
        }
        val schemaXml = """
<xs:schema elementFormDefault="unqualified" version="1.0"
${namespacesXml.prependIndent(" ".repeat(11))}
           targetNamespace="${partType.namespaceURI}">

    <xs:element name="$elementName" type="${partType.prefix}:${partType.localPart}" />
</xs:schema>
""".trim()

        logger.trace("Generated element schema:\n{}", schemaXml)
        return SchemaDocument.Factory.parse(schemaXml)
    }

    /**
     * @param parts map of element name to element qualified type
     */
    fun createCompositePartSchema(rootElement: QName, parts: Map<String, QName>): SchemaDocument {
        val namespaces = mutableMapOf<String, String>()
        if (rootElement.namespaceURI?.isNotBlank() == true) {
            namespaces[rootElement.prefix] = rootElement.namespaceURI
        }
        namespaces += parts.values.associate {
            val prefix = if (it.prefix.startsWith("ref:")) {
                it.prefix.substringAfter("ref:")
            } else {
                it.prefix
            }
            prefix to it.namespaceURI
        }
        if (!namespaces.containsKey("xs")) {
            namespaces["xs"] = "http://www.w3.org/2001/XMLSchema"
        }

        val namespacesXml = namespaces.entries.joinToString(separator = "\n") { (prefix, nsUri) ->
            """xmlns:${prefix}="${nsUri}""""
        }
        val partsXml = parts.entries.joinToString(separator = "\n") { (partName, partType) ->
            if (partType.prefix.startsWith("ref:")) {
                val refTarget = partType.prefix.substringAfter("ref:")
                """<xs:element ref="${refTarget}:${partType.localPart}"/>"""
            } else {
                """<xs:element name="$partName" type="${partType.prefix}:${partType.localPart}"/>"""
            }
        }
        val schemaXml = """
<xs:schema elementFormDefault="unqualified" version="1.0"
${namespacesXml.prependIndent(" ".repeat(11))}
           targetNamespace="${rootElement.namespaceURI}">

    <xs:element name="${rootElement.localPart}">
      <xs:complexType>
        <xs:sequence>
${partsXml.prependIndent(" ".repeat(10))}
        </xs:sequence>
      </xs:complexType>
    </xs:element>
</xs:schema>
""".trim()

        logger.trace("Generated element schema:\n{}", schemaXml)
        return SchemaDocument.Factory.parse(schemaXml)
    }
}
