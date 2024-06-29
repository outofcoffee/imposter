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

import io.gatehill.imposter.plugin.soap.model.CompositeOperationMessage
import io.gatehill.imposter.plugin.soap.model.ElementOperationMessage
import io.gatehill.imposter.plugin.soap.model.OperationMessage
import io.gatehill.imposter.plugin.soap.model.TypeOperationMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.xmlbeans.SchemaTypeSystem
import org.apache.xmlbeans.XmlBeans
import org.apache.xmlbeans.XmlError
import org.apache.xmlbeans.XmlException
import org.apache.xmlbeans.XmlOptions
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument
import org.xml.sax.EntityResolver
import javax.xml.namespace.QName

/**
 * Compiles schemas and generates synthetic schemas for entities.
 */
object SchemaUtil {
    private val logger: Logger = LogManager.getLogger(SchemaUtil::class.java)

    /**
     * Compiles the schemas, resolving against the base [SchemaTypeSystem] and
     * using the specified [EntityResolver], where necessary.
     */
    fun compileSchemas(
        baseSts: SchemaTypeSystem,
        entityResolver: EntityResolver,
        schemas: Array<SchemaDocument>,
    ): SchemaTypeSystem {
        if (schemas.isEmpty()) {
            logger.info("No schemas to compile")
        }

        val errors = mutableListOf<XmlError>()
        val compileOptions = XmlOptions()
            .setLoadLineNumbers()
            .setLoadMessageDigest()
            .setEntityResolver(entityResolver)
            .setErrorListener(errors)

        try {
            return XmlBeans.compileXsd(schemas, baseSts, compileOptions)
        } catch (e: Exception) {
            if (errors.isEmpty() || e !is XmlException) {
                throw RuntimeException("Error compiling schemas", e)
            }
            throw RuntimeException("Schema compilation errors: " + errors.joinToString("\n"))
        }
    }

    fun createSinglePartSchema(
        targetNamespace: String,
        part: OperationMessage,
    ): SchemaDocument {
        val namespaces = mutableMapOf<String, String>()
        val namespacesXml = generateNamespacesXml(namespaces, listOf(part))

        val partXml: String = convertMessage(part).joinToString("\n")

        // TODO replace with XMLCursor and XMLOptions.saveSyntheticDocumentElement
        // see: org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil#createSampleForType(org.apache.xmlbeans.SchemaField)
        val schemaXml = """
<xs:schema elementFormDefault="qualified" version="1.0"
${namespacesXml.prependIndent(" ".repeat(11))}
           targetNamespace="$targetNamespace">

${partXml.prependIndent(" ".repeat(4))}
</xs:schema>
""".trim()

        logger.trace("Generated single element schema:\n{}", schemaXml)
        return SchemaDocument.Factory.parse(schemaXml)
    }

    /**
     * @param parts map of element name to element qualified type
     */
    fun createCompositePartSchema(
        targetNamespace: String,
        rootElement: QName,
        parts: List<OperationMessage>,
    ): SchemaDocument {
        val namespaces = mutableMapOf<String, String>()
        if (rootElement.namespaceURI?.isNotBlank() == true) {
            namespaces[rootElement.prefix] = rootElement.namespaceURI
        }
        val namespacesXml = generateNamespacesXml(namespaces, parts)
        val partsXml = parts.flatMap { convertMessage(it) }.joinToString("\n")

        // TODO replace with XMLCursor and XMLOptions.saveSyntheticDocumentElement
        // see: org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil#createSampleForType(org.apache.xmlbeans.SchemaField)
        val schemaXml = """
<xs:schema elementFormDefault="qualified" version="1.0"
${namespacesXml.prependIndent(" ".repeat(11))}
           targetNamespace="$targetNamespace">

    <xs:element name="${rootElement.localPart}">
      <xs:complexType>
        <xs:sequence>
${partsXml.prependIndent(" ".repeat(10))}
        </xs:sequence>
      </xs:complexType>
    </xs:element>
</xs:schema>
""".trim()

        logger.trace("Generated composite element schema:\n{}", schemaXml)
        return SchemaDocument.Factory.parse(schemaXml)
    }

    private fun generateNamespacesXml(
        namespaces: MutableMap<String, String>,
        parts: List<OperationMessage>,
    ): String {
        parts.forEach { part -> part.namespaces.forEach { namespaces += it } }
        if (!namespaces.containsKey("xs")) {
            namespaces["xs"] = SoapUtil.NS_XML_SCHEMA
        }
        val namespacesXml = namespaces.entries.joinToString(separator = "\n") { (prefix, nsUri) ->
            """xmlns:${prefix}="${nsUri}""""
        }
        return namespacesXml
    }

    private fun convertMessage(message: OperationMessage): List<String> {
        val parts = mutableListOf<String>()

        when (message) {
            is ElementOperationMessage -> {
                // e.g. <element ref="tns:foo" />
                parts += """<xs:element ref="${message.elementName.prefix}:${message.elementName.localPart}"/>"""
            }

            is TypeOperationMessage -> {
                // e.g. <element name="foo" type="tns:fooType" />
                parts += """<xs:element name="${message.partName}" type="${message.typeName.prefix}:${message.typeName.localPart}"/>"""
            }

            is CompositeOperationMessage -> {
                parts += message.parts.flatMap { convertMessage(it) }
            }

            else -> throw UnsupportedOperationException(
                "Unsupported output message part: ${message::class.java.canonicalName}"
            )
        }

        return parts
    }
}
