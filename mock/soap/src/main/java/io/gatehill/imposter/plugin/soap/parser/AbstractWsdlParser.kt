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

package io.gatehill.imposter.plugin.soap.parser

import io.gatehill.imposter.plugin.soap.util.SchemaUtil
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.util.BodyQueryUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.xmlbeans.XmlBeans
import org.apache.xmlbeans.XmlOptions
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.XMLOutputter
import org.xml.sax.EntityResolver
import java.io.File
import javax.xml.namespace.QName

/**
 * @author Pete Cornish
 */
abstract class AbstractWsdlParser(
    private val wsdlFile: File,
    protected val document: Document,
    private val entityResolver: EntityResolver,
) : WsdlParser {
    protected val logger: Logger = LogManager.getLogger(this::class.java)

    override val schemaContext: SchemaContext by lazy {
        val schemas = discoverSchemas()
        val sts = SchemaUtil.compileSchemas(XmlBeans.getBuiltinTypeSystem(), entityResolver, schemas)
        SchemaContext(schemas, sts, entityResolver)
    }

    private val unionTypeSystem by lazy { XmlBeans.typeLoaderUnion(XmlBeans.getBuiltinTypeSystem(), schemaContext.sts) }

    private fun discoverSchemas(): Array<SchemaDocument> {
        val schemas = mutableListOf<SchemaDocument>()
        schemas += findEmbeddedTypesSchemas()

        // TODO consider only those referenced by 'xs:import' or 'xs:include'
        val xsds = wsdlFile.parentFile.listFiles { _, name -> name.endsWith(".xsd") }?.toList() ?: emptyList()
        schemas += xsds.map { schemaFile ->
            val options = XmlOptions()
                .setLoadLineNumbers()
                .setLoadMessageDigest()
                .setEntityResolver(entityResolver)

            SchemaDocument.Factory.parse(schemaFile, options)
        }

        logger.debug("Discovered {} schema(s) for WSDL: {}", schemas.size, wsdlFile)
        return schemas.toTypedArray()
    }

    private fun findEmbeddedTypesSchemas(): List<SchemaDocument> {
        val schemaNodes = findEmbeddedTypesSchemaNodes()
        if (schemaNodes.isEmpty()) {
            logger.warn("No embedded types schema found")
            return emptyList()
        }
        return schemaNodes.map { inlineSchemaElement ->
            val allNamespacesInScope = inlineSchemaElement.namespacesInScope

            // mutate a clone of the inline schema element
            val clonedInlineSchema = inlineSchemaElement.clone()

            // add parent namespaces to the inline schema
            allNamespacesInScope.forEach { ns ->
                if (clonedInlineSchema.namespacePrefix != ns.prefix && clonedInlineSchema.additionalNamespaces.none { it.prefix == ns.prefix }) {
                    clonedInlineSchema.addNamespaceDeclaration(ns)
                }
            }

            val schemaXml = XMLOutputter().outputString(clonedInlineSchema)
            logger.trace("Embedded types schema: {}", schemaXml)
            return@map SchemaDocument.Factory.parse(schemaXml)
        }
    }

    protected abstract fun findEmbeddedTypesSchemaNodes(): List<Element>

    protected fun selectSingleNodeWithName(context: Any, expressionTemplate: String, name: String): Element? {
        return selectSingleNode(context, String.format(expressionTemplate, name))
            ?: name.takeIf { it.contains(":") }?.let {
                selectSingleNode(context, String.format(expressionTemplate, SoapUtil.getLocalPart(name)))
            }
    }

    protected fun selectNodes(context: Any, expression: String): List<Element> =
        BodyQueryUtil.selectNodes(context, expression, xPathNamespaces)

    protected fun selectSingleNode(context: Any, expression: String): Element? =
        BodyQueryUtil.selectSingleNode(context, expression, xPathNamespaces)

    protected abstract val xPathNamespaces: List<Namespace>

    /**
     * Attempt to resolve the element with the given, optionally qualified, name
     * from within the XSD.
     */
    protected fun resolveElementTypeFromXsd(elementQName: QName): QName? {
        val matchingElement = schemaContext.sts.findElement(elementQName)
            ?: return null

        var elementType = matchingElement.type.name

        if (elementType.prefix.isNullOrBlank()) {
            val prefix = if (elementType.namespaceURI == SoapUtil.NS_XML_SCHEMA) {
                "xs"
            } else {
                // TODO consider prefix clashes - generate unique prefix?
                elementQName.prefix
            }
            elementType = QName(elementType.namespaceURI, elementType.localPart, prefix)
        }

        logger.trace("Resolved element name {} to qualified type: {}", elementQName, elementType)
        return elementType
    }

    /**
     * Attempt to resolve the type with the given, optionally qualified, name
     * from within the XSD.
     */
    protected fun resolveTypeFromXsd(typeQName: QName): QName? {
        var matchingType = unionTypeSystem.findType(typeQName)?.name
            ?: return null

        if (matchingType.prefix.isNullOrBlank()) {
            val prefix = if (matchingType.namespaceURI == SoapUtil.NS_XML_SCHEMA) {
                "xs"
            } else {
                // TODO consider prefix clashes - generate unique prefix?
                typeQName.prefix
            }
            matchingType = QName(matchingType.namespaceURI, matchingType.localPart, prefix)
        }

        logger.trace("Resolved type name {} to qualified type: {}", typeQName, matchingType)
        return matchingType
    }

    protected fun getAttributeValueAsQName(element: Element, attributeName: String): QName? {
        val attr = element.getAttribute(attributeName)
            ?: return null

        val attrValue = attr.value
            ?: return null

        val valueParts = attrValue.split(':')
        if (valueParts.size == 1) {
            // unqualified name
            return QName(valueParts[0])
        } else {
            val prefix = valueParts[0]
            val localName = valueParts[1]
            val ns = attr.namespacesInScope.find { it.prefix == prefix }?.uri
                ?: throw IllegalStateException("No namespace in scope with prefix '$prefix' for attribute '$attributeName' in element: $element")

            return QName(ns, localName, prefix)
        }
    }
}
