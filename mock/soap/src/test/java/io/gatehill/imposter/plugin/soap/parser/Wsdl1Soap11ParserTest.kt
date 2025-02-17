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

package io.gatehill.imposter.plugin.soap.parser

import io.gatehill.imposter.plugin.soap.model.BindingType
import io.gatehill.imposter.plugin.soap.model.ElementOperationMessage
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.namespace.QName

/**
 * Tests for [Wsdl1Parser] using a WSDL 1 document and SOAP 1.1.
 *
 * @author Pete Cornish
 */
class Wsdl1Soap11ParserTest {
    private lateinit var parser: Wsdl1Parser

    @BeforeEach
    fun setUp() {
        val wsdlFile = File(Wsdl1Soap11ParserTest::class.java.getResource("/wsdl1-soap11-document-bare/service.wsdl")!!.toURI())
        val document = SAXBuilder().build(wsdlFile)
        val entityResolver = WsdlRelativeXsdEntityResolver(wsdlFile.parentFile)
        parser = Wsdl1Parser(wsdlFile, document, entityResolver)
    }

    @Test
    fun getServices() {
        assertEquals(1, parser.services.size)

        val petService = parser.services.find { it.name == "PetService" }
        assertNotNull(petService, "PetService should not be null")

        petService!!
        assertEquals("PetService", petService.name)
        assertEquals(2, petService.endpoints.size)

        val soapEndpoint = petService.endpoints.filter { it.name == "SoapEndpoint" }.first()
        assertEquals("http://www.example.com/soap/", soapEndpoint.address.toASCIIString())
        assertEquals("tns:SoapBinding", soapEndpoint.bindingName)
    }

    @Test
    fun `get SOAP binding, getPetById operation`() {
        val binding = parser.getBinding("SoapBinding")
        assertNotNull(binding, "SoapBinding should not be null")

        binding!!
        assertEquals("SoapBinding", binding.name)
        assertEquals(BindingType.SOAP, binding.type)
        assertEquals("tns:PetPortType", binding.interfaceRef)

        assertEquals(2, binding.operations.size)
        val operation = binding.operations.find { it.name == "getPetById" }
        assertNotNull(operation, "getPetById operation should not be null")

        operation!!
        assertEquals("getPetById", operation.name)
        assertEquals("getPetById", operation.soapAction)

        // operation style defined at operation level in this service
        assertEquals("document", operation.style)

        assertEquals(
            QName("urn:com:example:petstore", "getPetByIdRequest"),
            (operation.inputRef as ElementOperationMessage).elementName,
        )
        assertEquals(
            QName("urn:com:example:petstore", "getPetByIdResponse"),
            (operation.outputRef as ElementOperationMessage).elementName,
        )
        assertEquals(
            QName("urn:com:example:petstore", "getPetFault"),
            (operation.faultRef as ElementOperationMessage?)?.elementName,
        )
    }

    @Test
    fun `get HTTP binding, getPetByName operation`() {
        val binding = parser.getBinding("HttpBinding")
        assertNotNull(binding, "HttpBinding should not be null")

        binding!!
        assertEquals("HttpBinding", binding.name)
        assertEquals(BindingType.HTTP, binding.type)
        assertEquals("tns:PetPortType", binding.interfaceRef)

        assertEquals(2, binding.operations.size)
        val operation = binding.operations.find { it.name == "getPetByName" }
        assertNotNull(operation, "getPetByName operation should not be null")

        operation!!
        assertEquals("getPetByName", operation.name)
        assertEquals("getPetByName", operation.soapAction)

        // operation style should fall back to binding style in WSDL 1.x
        assertEquals("document", operation.style)

        assertEquals(
            QName("urn:com:example:petstore", "getPetByNameRequest"),
            (operation.inputRef as ElementOperationMessage).elementName,
        )
        assertEquals(
            QName("urn:com:example:petstore", "getPetByNameResponse"),
            (operation.outputRef as ElementOperationMessage).elementName,
        )
        assertEquals(
            QName("urn:com:example:petstore", "getPetFault"),
            (operation.faultRef as ElementOperationMessage?)?.elementName,
        )
    }

    @Test
    fun getInterface() {
        val iface = parser.getInterface("PetPortType")
        assertNotNull(iface, "PetPortType should not be null")

        iface!!
        assertEquals("PetPortType", iface.name)
        assertEquals(2, iface.operationNames.size)
        assertEquals("getPetById", iface.operationNames.first())
    }

    @Test
    fun getSchemas() {
        // first schema is from the WSDL types element and just contains an import
        // second schema is the imported external XSD
        // third schema is the embedded schema from the WSDL types element
        assertEquals(3, parser.schemaContext.schemas.size)
    }
}
