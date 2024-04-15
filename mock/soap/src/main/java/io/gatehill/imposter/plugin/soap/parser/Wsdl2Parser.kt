/*
 * Copyright (c) 2022-2022.
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

import io.gatehill.imposter.plugin.soap.model.BindingType
import io.gatehill.imposter.plugin.soap.model.WsdlBinding
import io.gatehill.imposter.plugin.soap.model.WsdlEndpoint
import io.gatehill.imposter.plugin.soap.model.WsdlInterface
import io.gatehill.imposter.plugin.soap.model.WsdlOperation
import io.gatehill.imposter.plugin.soap.model.WsdlService
import io.gatehill.imposter.util.BodyQueryUtil
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.xml.sax.EntityResolver
import java.io.File
import java.net.URI
import javax.xml.namespace.QName

/**
 * WDSL 2.0 parser.
 *
 * @author Pete Cornish
 */
class Wsdl2Parser(
    wsdlFile: File,
    document: Document,
    entityResolver: EntityResolver,
) : AbstractWsdlParser(wsdlFile, document, entityResolver) {

    override val version = WsdlParser.WsdlVersion.V2

    override val services: List<WsdlService>
        get() {
            val services = selectNodes(document, "/wsdl:description/wsdl:service")

            return services.map { element ->
                WsdlService(
                    name = element.getAttributeValue("name"),
                    endpoints = getEndpoints(element),
                )
            }
        }

    override fun getBinding(bindingName: String): WsdlBinding? {
        val binding = selectSingleNodeWithName(
            context = document,
            expressionTemplate = "/wsdl:description/wsdl:binding[@name='%s']",
            name = bindingName
        ) ?: return null

        val interfaceName = binding.getAttributeValue("interface") ?: throw IllegalStateException(
            "Unable to find interface for binding $bindingName"
        )
        val operations = selectNodes(binding, "./wsdl:operation").map { op ->
            getOperation(interfaceName, op.getAttributeValue("ref")) ?: throw IllegalStateException(
                "Unable to find operation ${op.getAttributeValue("ref")} on interface $interfaceName"
            )
        }

        return WsdlBinding(
            name = binding.getAttributeValue("name"),
            type = parseBindingType(binding),
            interfaceRef = interfaceName,
            operations = operations,
        )
    }

    private fun parseBindingType(binding: Element) = when (binding.getAttributeValue("type")) {
        "http://www.w3.org/ns/wsdl/http" -> BindingType.HTTP
        "http://www.w3.org/ns/wsdl/soap" -> BindingType.SOAP
        else -> BindingType.UNKNOWN
    }

    override fun getInterface(interfaceName: String): WsdlInterface? {
        return getInterfaceNode(interfaceName)?.let { interfaceNode ->
            val operationNames = selectNodes(interfaceNode, "./wsdl:operation").map { node ->
                node.getAttributeValue("name")
            }

            WsdlInterface(
                name = interfaceName,
                operationNames = operationNames,
            )
        }
    }

    private fun getEndpoints(serviceNode: Element): List<WsdlEndpoint> {
        return selectNodes(serviceNode, "./wsdl:endpoint").map { endpoint ->
            WsdlEndpoint(
                name = endpoint.getAttributeValue("name"),
                bindingName = endpoint.getAttributeValue("binding"),
                address = URI(endpoint.getAttributeValue("address")),
            )
        }
    }

    private fun getOperation(interfaceName: String, operationName: String): WsdlOperation? {
        val interfaceNode: Element = getInterfaceNode(interfaceName) ?: throw IllegalStateException(
            "Unable to find interface $interfaceName"
        )
        val operation = selectSingleNodeWithName(
            context = interfaceNode,
            expressionTemplate = "./wsdl:operation[@name='%s']",
            name = operationName
        )
        return operation?.let { parseOperation(operation) }
    }

    private fun getInterfaceNode(interfaceName: String): Element? {
        return selectSingleNodeWithName(
            context = document,
            expressionTemplate = "/wsdl:description/wsdl:interface[@name='%s']",
            name = interfaceName
        )
    }

    private fun parseOperation(operation: Element): WsdlOperation {
        val soapOperation = selectSingleNode(operation, "./soap:operation") ?: throw IllegalStateException(
            "Unable to find soap:operation for operation ${operation.getAttributeValue("name")}"
        )
        val input = getMessagePartElementName(operation, "./wsdl:input")
        val output = getMessagePartElementName(operation, "./wsdl:output")

        return WsdlOperation(
            name = operation.getAttributeValue("name"),
            soapAction = soapOperation.getAttributeValue("soapAction"),
            style = soapOperation.getAttributeValue("style"),
            inputElementRef = input,
            outputElementRef = output,
        )
    }

    /**
     * Extract the WSDL message part element attribute, then attempt
     * to resolve it from within the XSD.
     */
    private fun getMessagePartElementName(context: Element, expression: String): QName? {
        val inputOrOutputNode = selectSingleNode(context, expression) ?: throw IllegalStateException(
            "Unable to find message part: $expression"
        )
        val elementName = inputOrOutputNode.getAttributeValue("element")
        return resolveElementFromXsd(elementName)
    }

    override val xPathNamespaces = listOf(
        Namespace.getNamespace("wsdl", wsdl2Namespace),
        Namespace.getNamespace("soap", "http://www.w3.org/ns/wsdl/soap"),
    )

    override fun findEmbeddedTypesSchemaNodes(): List<Element> {
        val xsNamespaces = xPathNamespaces + Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema")
        return BodyQueryUtil.selectNodes(document, "/wsdl:description/wsdl:types/xs:schema", xsNamespaces)
    }

    companion object {
        const val wsdl2Namespace = "http://www.w3.org/ns/wsdl"
    }
}
