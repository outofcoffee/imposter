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

import org.apache.xmlbeans.XmlObject
import org.jdom2.input.SAXBuilder
import java.io.File

/**
 * Detects WSDL version from namespaces of root element, then
 * delegates to version-specific [WsdlParser] implementation.
 *
 * @author Pete Cornish
 */
class VersionAwareWsdlParser(wsdlFile: File) : WsdlParser {
    private val delegate: WsdlParser

    init {
        val document = SAXBuilder().build(wsdlFile)
        val wsdlNamespaces = document.rootElement.namespacesInScope.filter {
            it.uri == Wsdl2Parser.wsdl2Namespace || it.uri == Wsdl1Parser.wsdl1Namespace
        }
        delegate = when (wsdlNamespaces.size) {
            0 -> throw IllegalStateException("No WSDL namespace found on root element")
            1 -> when (wsdlNamespaces.first().uri) {
                Wsdl1Parser.wsdl1Namespace -> Wsdl1Parser(wsdlFile, document)
                Wsdl2Parser.wsdl2Namespace -> Wsdl2Parser(wsdlFile, document)
                else -> throw IllegalStateException("Unsupported WSDL namespace on root element")
            }
            else -> throw IllegalStateException("More than one WSDL namespace found on root element: $wsdlNamespaces")
        }
    }

    override val version: WsdlParser.WsdlVersion
        get() = delegate.version

    override val schemas: Array<XmlObject>
        get() = delegate.schemas

    override val services: List<WsdlService>
        get() = delegate.services

    override fun getBinding(bindingName: String): WsdlBinding? =
        delegate.getBinding(bindingName)

    override fun getInterface(interfaceName: String): WsdlInterface? =
        delegate.getInterface(interfaceName)
}
