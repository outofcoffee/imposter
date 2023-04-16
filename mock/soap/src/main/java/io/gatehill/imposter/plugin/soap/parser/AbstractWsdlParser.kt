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

import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.util.BodyQueryUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.xmlbeans.SchemaTypeSystem
import org.apache.xmlbeans.XmlBeans
import org.apache.xmlbeans.XmlOptions
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.XMLOutputter
import java.io.File
import javax.xml.namespace.QName

/**
 * @author Pete Cornish
 */
abstract class AbstractWsdlParser(
    private val wsdlFile: File,
    protected val document: Document,
) : WsdlParser {
    protected val logger: Logger = LogManager.getLogger(this::class.java)

    override val schemas: Array<SchemaDocument> by lazy { discoverSchemas() }

    protected val xsd: SchemaTypeSystem by lazy { buildXsdFromSchemas() }

    private fun discoverSchemas(): Array<SchemaDocument> {
        val schemas = mutableListOf<SchemaDocument>()
        schemas += findEmbeddedTypesSchemas()

        // TODO consider only those referenced by 'xs:import'
        val xsds = wsdlFile.parentFile.listFiles { _, name -> name.endsWith(".xsd") }?.toList() ?: emptyList()
        schemas += xsds.map { schemaFile ->
            SchemaDocument.Factory.parse(schemaFile, XmlOptions().setLoadLineNumbers().setLoadMessageDigest())
        }

        logger.debug("Discovered ${schemas.size} schema(s) for WSDL: $wsdlFile")
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

    private fun buildXsdFromSchemas(): SchemaTypeSystem {
        if (schemas.isEmpty()) {
            throw IllegalStateException("Cannot build XSD from empty schema list")
        }
        val compileOptions = XmlOptions()
        try {
            return XmlBeans.compileXsd(schemas, XmlBeans.getBuiltinTypeSystem(), compileOptions)
        } catch (e: Exception) {
            throw RuntimeException("Schema compilation errors", e)
        }
    }

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
    protected fun resolveElementFromXsd(elementName: String): QName? {
        val localPart = SoapUtil.getLocalPart(elementName)

        // the top level element from the XSD
        val matchingTypeElement: QName? =
            // TODO should this be filtered on unqualified elements?
            xsd.documentTypes().find { it.documentElementName.localPart == localPart }?.documentElementName

        logger.trace("Resolved element name {} to qualified type: {}", elementName, matchingTypeElement)
        return matchingTypeElement
    }
}
