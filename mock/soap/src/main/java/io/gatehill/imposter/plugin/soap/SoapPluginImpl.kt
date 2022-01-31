/*
 * Copyright (c) 2016-2022.
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
package io.gatehill.imposter.plugin.soap

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.DefaultResponseBehaviourFactory
import io.gatehill.imposter.http.DefaultStatusCodeFactory
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.plugin.soap.config.SoapPluginConfig
import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.gatehill.imposter.plugin.soap.parser.VersionAwareWsdlParser
import io.gatehill.imposter.plugin.soap.parser.WsdlBinding
import io.gatehill.imposter.plugin.soap.parser.WsdlOperation
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import org.apache.xmlbeans.XmlObject
import org.apache.xmlbeans.impl.xsd2inst.SchemaInstanceGenerator
import java.io.File
import javax.inject.Inject
import javax.xml.namespace.QName
import kotlin.collections.set

/**
 * Plugin for SOAP.
 *
 * @author Pete Cornish
 */
@PluginInfo("soap")
@RequireModules(SoapModule::class)
class SoapPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val responseService: ResponseService,
) : ConfiguredPlugin<SoapPluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = SoapPluginConfig::class.java

    companion object {
        private val LOGGER = LogManager.getLogger(SoapPluginImpl::class.java)
    }

    override fun configureRoutes(router: HttpRouter) {
        if (configs.isEmpty()) {
            LOGGER.debug("No WSDL configuration files provided - skipping plugin setup")
            return
        }
        parseWsdls(router)
    }

    private fun parseWsdls(router: HttpRouter) {
        configs.forEach { config: SoapPluginConfig ->
            val fullWsdlPath = File(config.parentDir, config.wsdlFile!!)
            check(fullWsdlPath.exists())
            val wsdlParser = VersionAwareWsdlParser(fullWsdlPath)

            wsdlParser.services.forEach { service ->
                service.endpoints.forEach { endpoint ->
                    val path = endpoint.address.path
                    handlePathOperations(router, config, wsdlParser.schemas, path, wsdlParser.getBinding(endpoint.bindingName)!!)
                }
            }
        }
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the Vert.x router
     * @param config     the plugin configuration
     * @param schemas
     * @param path
     * @param binding
     */
    private fun handlePathOperations(
        router: HttpRouter,
        config: SoapPluginConfig,
        schemas: Array<XmlObject>,
        path: String,
        binding: WsdlBinding,
    ) {
        val fullPath = (config.path ?: "") + path
        LOGGER.debug("Adding mock endpoint: ${binding.name} -> $fullPath")

        // TODO parse HTTP binding to check for other verbs
        router.route(ResourceMethod.POST, fullPath).handler(
            resourceService.handleRoute(imposterConfig, config, vertx) { httpExchange: HttpExchange ->
                val soapEnv = httpExchange.body?.let { body ->
                    return@let SoapUtil.parseSoapEnvelope(body)

                } ?: run {
                    LOGGER.warn("No request body - unable to parse SOAP envelope")
                    httpExchange.response().setStatusCode(404).end()
                    return@handleRoute
                }

                val operation = determineOperation(binding, httpExchange, soapEnv) ?: run {
                    httpExchange.response().setStatusCode(404).end()
                    LOGGER.warn("Unable to find a matching binding operation using SOAPAction or SOAP request body")
                    return@handleRoute
                }
                check(operation.style.equals("document", ignoreCase = true)) {
                    "Only document SOAP bindings are supported"
                }

                LOGGER.debug("BindingOperation: ${operation.name}")
                handle(config, operation, schemas, httpExchange, soapEnv)
            }
        )
    }

    private fun determineOperation(binding: WsdlBinding, httpExchange: HttpExchange, soapEnv: ParsedSoapMessage): WsdlOperation? =
        determineOperationFromSoapAction(binding, httpExchange)
            ?: determineOperationFromRequestBody(binding, soapEnv)

    private fun determineOperationFromSoapAction(
        binding: WsdlBinding,
        httpExchange: HttpExchange
    ): WsdlOperation? {
        val soapAction = getSoapAction(httpExchange)
        soapAction ?: run {
            LOGGER.warn("Unable to find a SOAPAction")
            return null
        }
        LOGGER.trace("SOAPAction: $soapAction")

        val operation = binding.operations.firstOrNull { it.soapAction == soapAction }
        operation ?: run {
            LOGGER.debug("Unable to find a matching binding operation for SOAPAction: $soapAction")
        }
        return operation
    }

    private fun getSoapAction(httpExchange: HttpExchange): String? {
        val request = httpExchange.request()

        // e.g. SOAPAction: example
        return request.getHeader("SOAPAction") ?: run {
            // e.g. application/soap+xml;charset=UTF-8;action="example"
            val contentTypeParts = request.getHeader("Content-Type")?.split(";")?.map { it.trim() }

            contentTypeParts?.let { headerParts ->
                if (headerParts.any { it == "application/soap+xml" }) {
                    return@run headerParts.find { it.startsWith("action=") }
                }
                null
            }
        }
    }

    private fun determineOperationFromRequestBody(binding: WsdlBinding, soapEnv: ParsedSoapMessage): WsdlOperation? {
        soapEnv.soapBody ?: run {
            LOGGER.warn("Missing body in SOAP envelope")
            return null
        }
        val bodyRootElement = soapEnv.soapBody.children.firstOrNull() ?: run {
            LOGGER.warn("Missing element in SOAP body")
            return null
        }

        val matchedOps = binding.operations.filter { op ->
            op.inputElementRef?.namespaceURI == bodyRootElement.namespaceURI &&
                op.inputElementRef?.localPart == bodyRootElement.name
        }
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace(
                "Matched {} operations in binding {} based on body root element: {}",
                matchedOps.size,
                binding.name,
                matchedOps,
            )
        } else {
            LOGGER.debug(
                "Matched {} operations in binding {} based on body root element",
                matchedOps.size,
                binding.name,
            )
        }
        return when (matchedOps.size) {
            0 -> {
                LOGGER.warn("No operations found matching body root element: {}", bodyRootElement.qualifiedName)
                null
            }
            1 -> matchedOps.first()
            else -> {
                LOGGER.warn("Multiple operations found matching body root element: {}", bodyRootElement.qualifiedName)
                null
            }
        }
    }

    /**
     * Build a handler for the given operation.
     *
     * @param pluginConfig the plugin configuration
     * @param operation    the specification operation
     * @return a route handler
     */
    private fun handle(
        pluginConfig: SoapPluginConfig,
        operation: WsdlOperation,
        schemas: Array<XmlObject>,
        httpExchange: HttpExchange,
        soapEnv: ParsedSoapMessage,
    ) {
        val statusCodeFactory = DefaultStatusCodeFactory.instance
        val responseBehaviourFactory = DefaultResponseBehaviourFactory.instance

        val context = mapOf(
            "operation" to operation
        )

        val request = httpExchange.request()
        val resourceConfig = httpExchange.get<ResponseConfigHolder>(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY)

        val defaultBehaviourHandler = { responseBehaviour: ResponseBehaviour ->
            // set status code regardless of response strategy
            val response = httpExchange.response().setStatusCode(responseBehaviour.statusCode)

            findOutputTypeDefName(operation)?.let { outputTypeDefName ->
                if (!responseBehaviour.responseHeaders.containsKey(HttpUtil.CONTENT_TYPE)) {
                    responseBehaviour.responseHeaders[HttpUtil.CONTENT_TYPE] = "application/soap+xml"
                }

                // build a response from the specification
                val exampleSender =
                    ResponseSender { httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour ->
                        serveExample(httpExchange, schemas, outputTypeDefName, soapEnv)
                    }

                // attempt to serve an example from the specification, falling back if not present
                responseService.sendResponse(
                    pluginConfig,
                    resourceConfig,
                    httpExchange,
                    responseBehaviour,
                    exampleSender,
                )

            } ?: run {
                LOGGER.warn(
                    "No output type definition found in specification for {} {} and status code {}",
                    request.method(),
                    request.path(),
                    responseBehaviour.statusCode
                )
                response.end()
            }
        }

        responseService.handle(
            pluginConfig,
            resourceConfig,
            httpExchange,
            context,
            statusCodeFactory,
            responseBehaviourFactory,
            defaultBehaviourHandler
        )
    }

    /**
     * @return an API response
     */
    private fun findOutputTypeDefName(operation: WsdlOperation): QName? {
        val outputTypeDef = operation.outputElementRef
        LOGGER.debug("Using output schema type named: $outputTypeDef")
        return outputTypeDef
    }

    private fun serveExample(
        httpExchange: HttpExchange,
        schemas: Array<XmlObject>,
        outputTypeDefName: QName,
        soapEnv: ParsedSoapMessage,
    ): Boolean {
        LOGGER.debug("Generating example for $outputTypeDefName")

        val example = SchemaInstanceGenerator.xsd2inst(
            schemas,
            outputTypeDefName.localPart,
            SchemaInstanceGenerator.Xsd2InstOptions()
        )

        transmitExample(httpExchange, example, soapEnv)
        return true
    }

    private fun transmitExample(httpExchange: HttpExchange, example: String?, soapEnv: ParsedSoapMessage) {
        example ?: run {
            LOGGER.info("No example found - returning empty response")
            httpExchange.response().end()
            return
        }

        val responseBody = SoapUtil.wrapInEnv(example, soapEnv.soapEnvNamespace)
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace(
                "Serving mock example for URI {} with status code {}: {}",
                httpExchange.request().absoluteURI(),
                httpExchange.response().getStatusCode(),
                responseBody
            )
        } else {
            LOGGER.info(
                "Serving mock example for URI {} with status code {} (response body {} bytes)",
                httpExchange.request().absoluteURI(),
                httpExchange.response().getStatusCode(),
                responseBody.length
            )
        }
        httpExchange.response()
            .putHeader(HttpUtil.CONTENT_TYPE, "application/soap+xml")
            .end(responseBody)
    }
}
