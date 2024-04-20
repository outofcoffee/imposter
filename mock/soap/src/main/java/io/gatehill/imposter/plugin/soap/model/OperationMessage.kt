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

package io.gatehill.imposter.plugin.soap.model

import javax.xml.namespace.QName

/**
 * Represents an operation message. In WSDL 1.1 this is an individual
 * `part` element within a `message`. In WSDL 2.0 this is an `input` or `output`
 * element within an `operation`.
 */
abstract class OperationMessage(
    val namespaces: List<Map<String, String>>
)

/**
 * Refers to an XML schema `element`.
 */
class ElementOperationMessage(
    namespaces: List<Map<String, String>>,
    val elementName: QName,
) : OperationMessage(
    namespaces = namespaces
) {
    override fun toString(): String =
        "ElementOperationMessage(elementName=$elementName)"
}

/**
 * Message parts specifying an XML schema `type` are supported
 * in WSDL 1.1 but not in WSDL 2.0.
 */
class TypeOperationMessage(
    namespaces: List<Map<String, String>>,
    val partName: String,
    val typeName: QName,
): OperationMessage(
    namespaces = namespaces
) {
    override fun toString(): String =
        "TypeOperationMessage(partName='$partName', typeName=$typeName)"
}

/**
 * In WSDL 1.1, messages can define multiple parts.
 */
class CompositeOperationMessage(
    /**
     * Maps the `part` name to an operation message.
     */
    val parts: List<OperationMessage>
): OperationMessage(
    namespaces = emptyList()
) {
    override fun toString(): String =
        "CompositeOperationMessage(parts=$parts)"
}
