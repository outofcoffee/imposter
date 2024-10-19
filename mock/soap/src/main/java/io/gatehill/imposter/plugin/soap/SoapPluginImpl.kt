/*
 * Copyright (c) 2016-2024.
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
import io.gatehill.imposter.http.DefaultStatusCodeFactory
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.soap.config.SoapPluginConfig
import io.gatehill.imposter.plugin.soap.http.SoapResponseBehaviourFactory
import io.gatehill.imposter.plugin.soap.model.BindingType
import io.gatehill.imposter.plugin.soap.model.MessageBodyHolder
import io.gatehill.imposter.plugin.soap.model.OperationMessage
import io.gatehill.imposter.plugin.soap.model.ParsedRawBody
import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.gatehill.imposter.plugin.soap.model.WsdlBinding
import io.gatehill.imposter.plugin.soap.model.WsdlOperation
import io.gatehill.imposter.plugin.soap.model.WsdlService
import io.gatehill.imposter.plugin.soap.parser.VersionAwareWsdlParser
import io.gatehill.imposter.plugin.soap.parser.WsdlParser
import io.gatehill.imposter.plugin.soap.service.SoapExampleService
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.DefaultBehaviourHandler
import io.gatehill.imposter.service.HandlerService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.completedUnitFuture
import io.gatehill.imposter.util.makeFuture
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
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
    private val handlerService: HandlerService,
    private val responseService: ResponseService,
    private val responseRoutingService: ResponseRoutingService,
    private val soapExampleService: SoapExampleService,
    private val soapResponseBehaviourFactory: SoapResponseBehaviourFactory,
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
            val fullWsdlPath = File(config.dir, config.wsdlFile!!)
            check(fullWsdlPath.exists()) {
                "WSDL file not found at path: $fullWsdlPath"
            }
            val wsdlParser = VersionAwareWsdlParser(fullWsdlPath)

            // TODO optionally support ?wsdl query to return the WSDL

            wsdlParser.services.forEach { service ->
                service.endpoints.forEach { endpoint ->
                    val path = endpoint.address.path
                    val binding = wsdlParser.getBinding(endpoint.bindingName)
                        ?: throw IllegalStateException("Binding: ${endpoint.bindingName} not found")

                    when (binding.type) {
                        BindingType.SOAP, BindingType.HTTP -> {
                            handleBindingOperations(router, config, wsdlParser, path, service, binding)
                        }

                        else -> LOGGER.debug("Ignoring unsupported binding: ${binding.name}")
                    }
                }
            }
        }
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the HTTP router
     * @param config     the plugin configuration
     * @param parser     the WSDL parser
     * @param path       the path of this service
     * @param binding    the binding
     */
    private fun handleBindingOperations(
        router: HttpRouter,
        config: SoapPluginConfig,
        parser: WsdlParser,
        path: String,
        service: WsdlService,
        binding: WsdlBinding,
    ) {
        val fullPath = (config.path ?: "") + path
        LOGGER.debug("Adding mock endpoint: ${binding.name} -> ${binding.type} $fullPath")

        val soapResourceMatcher = SoapResourceMatcher(binding)

        // TODO parse HTTP binding to check for other verbs
        router.route(HttpMethod.POST, fullPath).handler(
            handlerService.build(imposterConfig, config, soapResourceMatcher) { httpExchange: HttpExchange ->
                val bodyHolder: MessageBodyHolder = when (binding.type) {
                    BindingType.SOAP, BindingType.HTTP -> {
                        httpExchange.request.body?.let { body -> SoapUtil.parseBody(config, body) } ?: run {
                            LOGGER.warn("No request body - unable to parse SOAP message")
                            httpExchange.response.setStatusCode(400).end()
                            return@build completedUnitFuture()
                        }
                    }

                    else -> {
                        LOGGER.warn("Unsupported binding type: ${binding.type} - unable to parse request")
                        httpExchange.response.setStatusCode(400).end()
                        return@build completedUnitFuture()
                    }
                }

                val soapAction = soapResourceMatcher.getSoapAction(httpExchange)
                val operation = soapResourceMatcher.determineOperation(soapAction, bodyHolder) ?: run {
                    httpExchange.response.setStatusCode(404).end()
                    LOGGER.warn("Unable to find a matching binding operation using SOAPAction or SOAP request body")
                    return@build completedUnitFuture()
                }
                check(
                    operation.style.equals(SoapUtil.OPERATION_STYLE_DOCUMENT, ignoreCase = true)
                            || operation.style.equals(SoapUtil.OPERATION_STYLE_RPC, ignoreCase = true)
                ) {
                    "Only document and RPC style SOAP bindings are supported"
                }

                LOGGER.debug("Matched operation: ${operation.name} in binding: ${binding.name}")
                return@build handle(config, parser, service, binding, operation, httpExchange, bodyHolder, soapAction)
            }
        )
    }

    /**
     * Handle a request for the given operation.
     *
     * @param pluginConfig the plugin configuration
     * @param parser       the WSDL parser
     * @param binding      the WSDL binding
     * @param operation    the WSDL operation
     * @param httpExchange the current exchange
     * @param bodyHolder   the holder of the body, such as a SOAP envelope or raw HTTP request body
     * @param soapAction   the SOAPAction, if present
     */
    private fun handle(
        pluginConfig: SoapPluginConfig,
        parser: WsdlParser,
        service: WsdlService,
        binding: WsdlBinding,
        operation: WsdlOperation,
        httpExchange: HttpExchange,
        bodyHolder: MessageBodyHolder,
        soapAction: String?,
    ): CompletableFuture<Unit> {
        val statusCodeFactory = DefaultStatusCodeFactory.instance
        val resourceConfig = httpExchange.get<BasicResourceConfig>(ResourceUtil.RESOURCE_CONFIG_KEY)

        val defaultBehaviourHandler: DefaultBehaviourHandler = { responseBehaviour: ResponseBehaviour ->
            // set status code regardless of response strategy
            val response = httpExchange.response
                .setStatusCode(responseBehaviour.statusCode)

            determineResponseMessage(responseBehaviour, operation)?.let { message ->
                LOGGER.trace("Using output schema type: {}", message)

                if (!responseBehaviour.responseHeaders.containsKey(HttpUtil.CONTENT_TYPE)) {
                    responseBehaviour.responseHeaders[HttpUtil.CONTENT_TYPE] = when (bodyHolder) {
                        is ParsedSoapMessage -> when (parser.version) {
                            WsdlParser.WsdlVersion.V1 -> SoapUtil.soap11ContentType
                            WsdlParser.WsdlVersion.V2 -> SoapUtil.soap12ContentType
                        }

                        is ParsedRawBody -> SoapUtil.textXmlContentType
                        else -> throw IllegalStateException("Unsupported request body: ${bodyHolder::class.java.canonicalName}")
                    }
                }

                // build a response from the XSD
                val exampleSender = ResponseSender { httpExchange: HttpExchange, _: ResponseBehaviour ->
                    soapExampleService.serveExample(
                        httpExchange,
                        parser.schemaContext,
                        service,
                        operation,
                        message,
                        bodyHolder
                    )
                }

                // attempt to serve the example, falling back if not present
                return@let responseService.sendResponse(
                    pluginConfig,
                    resourceConfig,
                    httpExchange,
                    responseBehaviour,
                    exampleSender,
                )

            } ?: run {
                LOGGER.warn(
                    "No output or fault definition found in WSDL for {} and status code {}",
                    LogUtil.describeRequestShort(httpExchange),
                    responseBehaviour.statusCode,
                )
                makeFuture { response.end() }
            }
        }

        val context = mutableMapOf(
            "binding" to binding,
            "operation" to operation,
        )
        soapAction?.let { context["soapAction"] = it }

        return responseRoutingService.route(
            pluginConfig,
            resourceConfig,
            httpExchange,
            context,
            statusCodeFactory,
            soapResponseBehaviourFactory,
            defaultBehaviourHandler,
        )
    }

    private fun determineResponseMessage(
        responseBehaviour: ResponseBehaviour,
        operation: WsdlOperation
    ): OperationMessage? {
        return when {
            responseBehaviour.soapFault -> operation.faultRef
            responseBehaviour.statusCode == HttpUtil.HTTP_INTERNAL_ERROR -> operation.faultRef
            else -> operation.outputRef
        }
    }
}
