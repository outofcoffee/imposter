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
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.util.BodyQueryUtil
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import java.io.File
import java.net.URI
import javax.xml.namespace.QName

/**
 * WSDL 1.x parser.
 *
 * @author Pete Cornish
 */
class Wsdl1Parser(
    wsdlFile: File,
    document: Document,
) : AbstractWsdlParser(wsdlFile, document) {

    override val version = WsdlParser.WsdlVersion.V1

    override val services: List<WsdlService>
        get() {
            val services = selectNodes(document, "/wsdl:definitions/wsdl:service")

            return services.map { element ->
                WsdlService(
                    name = element.getAttributeValue("name"),
                    endpoints = getPorts(element),
                )
            }
        }

    override fun getBinding(bindingName: String): WsdlBinding? {
        val binding = getBindingElement(bindingName)
            ?: return null

        val operations = selectNodes(binding, "./wsdl:operation").map { op ->
            getOperation(bindingName, op.getAttributeValue("name"), binding)!!
        }

        return WsdlBinding(
            name = binding.getAttributeValue("name"),

            type = parseBindingType(binding),

            // binding type=portType name
            interfaceRef = binding.getAttributeValue("type")!!,

            operations = operations,
        )
    }

    private fun parseBindingType(binding: Element): BindingType {
        val soapBinding = selectSingleNode(binding, "./soap:binding")
            ?: selectSingleNode(binding, "./soap12:binding")

        return when (soapBinding?.getAttributeValue("transport")) {
            "http://schemas.xmlsoap.org/soap/http" -> BindingType.HTTP
            "http://schemas.xmlsoap.org/soap/soap" -> BindingType.SOAP
            else -> BindingType.UNKNOWN
        }
    }

    /**
     * WSDL 1.x portType
     */
    override fun getInterface(interfaceName: String): WsdlInterface? {
        return getPortTypeNode(interfaceName)?.let { portTypeNode ->
            val operationNames = selectNodes(portTypeNode, "./wsdl:operation").map { node ->
                node.getAttributeValue("name")
            }

            WsdlInterface(
                name = interfaceName,
                operationNames = operationNames,
            )
        }
    }

    private fun getPorts(serviceNode: Element): List<WsdlEndpoint> {
        return selectNodes(serviceNode, "./wsdl:port").map { port ->
            val soapAddress = selectSingleNode(port, "./soap:address")
                ?: selectSingleNode(port, "./soap12:address")
                ?: throw IllegalStateException("No SOAP address element found for port: ${describeNode(port)}")

            WsdlEndpoint(
                name = port.getAttributeValue("name"),
                bindingName = port.getAttributeValue("binding"),
                address = URI(soapAddress.getAttributeValue("location")),
            )
        }
    }

    private fun getOperation(bindingName: String, operationName: String, bindingNode: Element? = null): WsdlOperation? {
        val binding = bindingNode ?: getBindingElement(bindingName) ?: return null

        val bindingOperation = selectSingleNodeWithName(
            context = binding,
            expressionTemplate = "./wsdl:operation[@name='%s']",
            name = operationName
        )!!
        val soapOperation = selectSingleNode(bindingOperation, "./soap:operation")
            ?: selectSingleNode(bindingOperation, "./soap12:operation")
            ?: throw IllegalStateException("No SOAP operation element found for binding operation: ${describeNode(bindingOperation)}")

        // binding type=portType name
        val portTypeName = binding.getAttributeValue("type")
        val portType = getPortTypeNode(portTypeName)!!
        val portTypeOperation = selectSingleNodeWithName(
            context = portType,
            expressionTemplate = "./wsdl:operation[@name='%s']",
            name = operationName
        )!!
        val input = getMessagePartElementName(portTypeOperation, "./wsdl:input")!!
        val output = getMessagePartElementName(portTypeOperation, "./wsdl:output")!!

        val style = soapOperation.getAttributeValue("style") ?: run {
            // fall back to soap:binding
            val soapBinding = selectSingleNode(binding, "./soap:binding")
                ?: selectSingleNode(binding, "./soap12:binding")

            soapBinding?.getAttributeValue("style")
        }

        return WsdlOperation(
            name = bindingOperation.getAttributeValue("name"),
            soapAction = soapOperation.getAttributeValue("soapAction"),
            style = style,
            inputElementRef = input,
            outputElementRef = output,
        )
    }

    private fun getBindingElement(bindingName: String) = selectSingleNodeWithName(
        context = document,
        expressionTemplate = "/wsdl:definitions/wsdl:binding[@name='%s']",
        name = bindingName
    )

    private fun getPortTypeNode(portTypeName: String): Element? {
        return selectSingleNodeWithName(
            context = document,
            expressionTemplate = "/wsdl:definitions/wsdl:portType[@name='%s']",
            name = portTypeName
        )
    }

    /**
     * Extract the WSDL message part element attribute, then attempt
     * to resolve it from within the XSD.
     */
    private fun getMessagePartElementName(context: Element, expression: String): QName? {
        val inputOrOutputNode = selectSingleNode(context, expression)!!
        val msgAttr = inputOrOutputNode.getAttributeValue("message")!!
        val messageName = msgAttr.let { SoapUtil.getLocalPart(msgAttr) }

        // look up message
        val messagePart = selectSingleNode(document, "/wsdl:definitions/wsdl:message[@name='$messageName']/wsdl:part")
            ?: throw IllegalStateException("Message $msgAttr not found")

        val elementName = messagePart.getAttributeValue("element")
        return resolveElementFromXsd(elementName)
    }

    override val xPathNamespaces = listOf(
        Namespace.getNamespace("wsdl", wsdl1Namespace),
        Namespace.getNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/"),
        Namespace.getNamespace("soap12", "http://schemas.xmlsoap.org/wsdl/soap12/"),
    )

    override fun findEmbeddedTypesSchemaNodes(): List<Element> {
        val xsNamespaces = xPathNamespaces + Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema")
        return BodyQueryUtil.selectNodes(document, "/wsdl:definitions/wsdl:types/xs:schema", xsNamespaces)
    }

    /**
     * @return a human-readable description of the node
     */
    private fun describeNode(node: Element): String = node.getAttributeValue("name") ?: node.toString()

    companion object {
        const val wsdl1Namespace = "http://schemas.xmlsoap.org/wsdl/"
    }
}
