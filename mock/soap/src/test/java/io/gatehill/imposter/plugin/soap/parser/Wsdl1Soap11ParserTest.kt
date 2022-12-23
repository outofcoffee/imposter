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
import org.jdom2.input.SAXBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.xml.namespace.QName

/**
 * Tests for [Wsdl1Parser] using a WSDL 1 document and SOAP 1.1.
 *
 * @author Pete Cornish
 */
class Wsdl1Soap11ParserTest {
    private lateinit var parser: Wsdl1Parser

    @Before
    fun setUp() {
        val wsdlFile = File(Wsdl1Soap11ParserTest::class.java.getResource("/wsdl1-soap11/service.wsdl")!!.toURI())
        val document = SAXBuilder().build(wsdlFile)
        parser = Wsdl1Parser(wsdlFile, document)
    }

    @Test
    fun getServices() {
        assertEquals(1, parser.services.size)

        val petService = parser.services.find { it.name == "PetService" }
        assertNotNull("PetService should not be null", petService)

        petService!!
        assertEquals("PetService", petService.name)
        assertEquals(2, petService.endpoints.size)

        val soapEndpoint = petService.endpoints.filter { it.name == "SoapEndpoint" }.first()
        assertEquals("http://www.example.com/soap/", soapEndpoint.address.toASCIIString())
        assertEquals("tns:SoapBinding", soapEndpoint.bindingName)
    }

    @Test
    fun getBinding() {
        val binding = parser.getBinding("SoapBinding")
        assertNotNull("SoapBinding should not be null", binding)

        binding!!
        assertEquals("SoapBinding", binding.name)
        assertEquals(BindingType.SOAP, binding.type)
        assertEquals("tns:PetPortType", binding.interfaceRef)

        assertEquals(2, binding.operations.size)
        val operation = binding.operations.find { it.name == "getPetById" }
        assertNotNull("getPetById operation should not be null", operation)

        operation!!
        assertEquals("getPetById", operation.name)
        assertEquals("getPetById", operation.soapAction)
        assertEquals("document", operation.style)
        assertEquals(QName("urn:com:example:petstore","getPetByIdRequest"), operation.inputElementRef)
        assertEquals(QName("urn:com:example:petstore","getPetByIdResponse"), operation.outputElementRef)

        // operation style should fall back to binding style in WSDL 1.x
        val petNameOp = binding.operations.find { it.name == "getPetByName" }
        assertNotNull(petNameOp)
        assertEquals("document", petNameOp?.style)
    }

    @Test
    fun getInterface() {
        val iface = parser.getInterface("PetPortType")
        assertNotNull("PetPortType should not be null", iface)

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
        assertEquals(3, parser.schemas.size)
    }
}
