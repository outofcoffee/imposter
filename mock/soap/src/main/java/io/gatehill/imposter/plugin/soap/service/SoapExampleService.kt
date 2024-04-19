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

package io.gatehill.imposter.plugin.soap.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.soap.model.ElementOperationMessage
import io.gatehill.imposter.plugin.soap.model.MessageBodyHolder
import io.gatehill.imposter.plugin.soap.model.OperationMessage
import io.gatehill.imposter.plugin.soap.model.ParsedRawBody
import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.gatehill.imposter.plugin.soap.model.TypeOperationMessage
import io.gatehill.imposter.plugin.soap.model.WsdlService
import io.gatehill.imposter.plugin.soap.parser.WsdlRelativeXsdEntityResolver
import io.gatehill.imposter.plugin.soap.util.SchemaGenerator
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.xmlbeans.SchemaType
import org.apache.xmlbeans.SchemaTypeSystem
import org.apache.xmlbeans.XmlBeans
import org.apache.xmlbeans.XmlError
import org.apache.xmlbeans.XmlException
import org.apache.xmlbeans.XmlOptions
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument
import org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil
import java.io.File
import javax.xml.namespace.QName


/**
 * Serves an example of a given output type from a schema.
 *
 * @author Pete Cornish
 */
class SoapExampleService {
    private val logger: Logger = LogManager.getLogger(SoapExampleService::class.java)

    fun serveExample(
        httpExchange: HttpExchange,
        schemas: Array<SchemaDocument>,
        wsdlDir: File,
        service: WsdlService,
        outputMessage: OperationMessage,
        bodyHolder: MessageBodyHolder,
    ): Boolean {
        logger.debug("Generating example for {}", outputMessage)
        val example = generateInstanceFromSchemas(schemas, wsdlDir, service, outputMessage)
        transmitExample(httpExchange, example, bodyHolder)
        return true
    }

    private fun generateInstanceFromSchemas(
        schemas: Array<SchemaDocument>,
        wsdlDir: File,
        service: WsdlService,
        message: OperationMessage,
    ): String {
        when (message) {
            is ElementOperationMessage -> {
                val sts: SchemaTypeSystem = buildSchemaTypeSystem(wsdlDir, schemas)

                // TODO should this use the qualified name instead?
                val rootElementName = message.elementName.localPart
                val elem: SchemaType = sts.documentTypes().find { it.documentElementName.localPart == rootElementName }
                    ?: throw RuntimeException("Could not find a global element with name \"$rootElementName\"")

                return SampleXmlUtil.createSampleForType(elem)
            }

            is TypeOperationMessage -> {
                // by convention, the suffix 'Response' is added to the operation name
                val elementName = message.operationName + "Response"
                val rootElement = if (service.targetNamespace?.isNotBlank() == true) {
                    QName(service.targetNamespace, elementName, "tns")
                } else {
                    QName(elementName)
                }

                val parts = mutableMapOf<String, QName>()
                parts[message.partName] = message.typeName

                val elementSchema = SchemaGenerator.createElementSchema(rootElement, parts)
                val sts: SchemaTypeSystem = buildSchemaTypeSystem(wsdlDir, schemas + elementSchema)

                val elem = sts.documentTypes().find { it.documentElementName == rootElement }
                    ?: throw RuntimeException("Could not find a generated element with name \"$rootElement\"")

                return SampleXmlUtil.createSampleForType(elem)
            }

            // TODO support CompositeOperationMessage
            else -> throw UnsupportedOperationException("Unsupported output message: ${message::class.java.canonicalName}")
        }
    }

    private fun buildSchemaTypeSystem(
        wsdlDir: File,
        schemas: Array<SchemaDocument>,
    ): SchemaTypeSystem {
        var sts: SchemaTypeSystem? = null
        if (schemas.isNotEmpty()) {
            val errors = mutableListOf<XmlError>()

            val compileOptions = XmlOptions()
                .setLoadLineNumbers()
                .setLoadMessageDigest()
                .setEntityResolver(WsdlRelativeXsdEntityResolver(wsdlDir))
                .setErrorListener(errors)

            try {
                // TODO consider reusing the SchemaTypeSystem from [AbstractWsdlParser.buildXsdFromSchemas]
                sts = XmlBeans.compileXsd(schemas, XmlBeans.getBuiltinTypeSystem(), compileOptions)
            } catch (e: Exception) {
                if (errors.isEmpty() || e !is XmlException) {
                    throw RuntimeException("Error compiling XSD", e)
                }
                throw RuntimeException("Schema compilation errors: " + errors.joinToString("\n"))
            }
        }
        sts ?: throw IllegalStateException("No schemas to process")
        return sts
    }

    private fun transmitExample(httpExchange: HttpExchange, example: String?, bodyHolder: MessageBodyHolder) {
        example ?: run {
            logger.warn("No example found - returning empty response")
            httpExchange.response.end()
            return
        }

        val responseBody = when (bodyHolder) {
            is ParsedSoapMessage -> SoapUtil.wrapInEnv(example, bodyHolder.soapEnvNamespace)
            is ParsedRawBody -> example
            else -> throw IllegalStateException("Unsupported request body: ${bodyHolder::class.java.canonicalName}")
        }
        if (logger.isTraceEnabled) {
            logger.trace(
                "Serving mock example for {} with status code {}: {}",
                LogUtil.describeRequestShort(httpExchange),
                httpExchange.response.statusCode,
                responseBody
            )
        } else {
            logger.info(
                "Serving mock example for {} with status code {} (response body {} bytes)",
                LogUtil.describeRequestShort(httpExchange),
                httpExchange.response.statusCode,
                responseBody.length
            )
        }
        httpExchange.response.end(responseBody)
    }
}
