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

package io.gatehill.imposter.plugin.soap

import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.http.AbstractResourceMatcher
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.ResourceMatchResult
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.soap.config.SoapPluginConfig
import io.gatehill.imposter.plugin.soap.config.SoapPluginResourceConfig
import io.gatehill.imposter.plugin.soap.model.BindingType
import io.gatehill.imposter.plugin.soap.model.ElementOperationMessage
import io.gatehill.imposter.plugin.soap.model.MessageBodyHolder
import io.gatehill.imposter.plugin.soap.model.TypeOperationMessage
import io.gatehill.imposter.plugin.soap.model.WsdlBinding
import io.gatehill.imposter.plugin.soap.model.WsdlOperation
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import org.apache.logging.log4j.LogManager

/**
 * SOAP specific matcher, for a particular binding, operation or action.
 *
 * @author Pete Cornish
 */
class SoapResourceMatcher(
    private val binding: WsdlBinding,
) : AbstractResourceMatcher() {

    /**
     * {@inheritDoc}
     */
    override fun matchRequest(
        pluginConfig: PluginConfig,
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource {
        val resourceConfig = resource.config as SoapPluginResourceConfig
        val soapAction = getSoapAction(httpExchange)

        val matchResults = listOf(
            matchPath(httpExchange, resourceConfig, httpExchange.request),
            matchSoapAction(resourceConfig, soapAction),
            matchBinding(resourceConfig),
            matchOperation(resourceConfig, pluginConfig, httpExchange, soapAction),
            matchRequestBody(httpExchange, pluginConfig, resource.config),
            matchEval(httpExchange, pluginConfig, resource),
        )
        return determineMatch(matchResults, resource, httpExchange)
    }

    fun getSoapAction(httpExchange: HttpExchange): String? {
        val request = httpExchange.request

        val soapAction: String? = getSoapActionHeader(request)
            ?: getSoapActionFromContentType(request)

        soapAction ?: LOGGER.trace("No SOAPAction found")
        return soapAction
    }

    private fun matchSoapAction(
        resourceConfig: SoapPluginResourceConfig,
        soapAction: String?,
    ): ResourceMatchResult {
        val matchDescription = "SOAP action"
        return resourceConfig.soapAction?.let {
            if (it == soapAction) {
                ResourceMatchResult.exactMatch(matchDescription)
            } else {
                ResourceMatchResult.notMatched(matchDescription)
            }
        } ?: ResourceMatchResult.noConfig(matchDescription)
    }

    private fun matchBinding(
        resourceConfig: SoapPluginResourceConfig,
    ): ResourceMatchResult {
        val matchDescription = "SOAP binding"
        return resourceConfig.binding?.let {
            if (it == binding.name) {
                ResourceMatchResult.exactMatch(matchDescription)
            } else {
                ResourceMatchResult.notMatched(matchDescription)
            }
        } ?: ResourceMatchResult.noConfig(matchDescription)
    }

    private fun matchOperation(
        resourceConfig: SoapPluginResourceConfig,
        pluginConfig: PluginConfig,
        httpExchange: HttpExchange,
        soapAction: String?,
    ): ResourceMatchResult {
        val matchDescription = "SOAP operation"
        return resourceConfig.operation?.let {
            if (isOperationMatch(pluginConfig, httpExchange, it, soapAction)) {
                ResourceMatchResult.exactMatch(matchDescription)
            } else {
                ResourceMatchResult.notMatched(matchDescription)
            }
        } ?: ResourceMatchResult.noConfig(matchDescription)
    }

    private fun isOperationMatch(
        config: PluginConfig,
        httpExchange: HttpExchange,
        configOpName: String,
        soapAction: String?,
    ) = httpExchange.request.body?.let { body ->
        val bodyHolder: MessageBodyHolder = when (binding.type) {
            BindingType.SOAP, BindingType.HTTP -> SoapUtil.parseBody(config as SoapPluginConfig, body)
            else -> {
                LOGGER.warn("Unsupported binding type: ${binding.type} - unable to determine operation match")
                return false
            }
        }
        val operation = determineOperation(soapAction, bodyHolder)
        configOpName == operation?.name
    } ?: false

    fun determineOperation(soapAction: String?, bodyHolder: MessageBodyHolder): WsdlOperation? {
        soapAction?.let {
            return binding.operations.firstOrNull { it.soapAction == soapAction }
        }
        return determineOperationFromRequestBody(bodyHolder)
    }

    private fun getSoapActionHeader(request: HttpRequest): String? {
        request.getHeader("SOAPAction")?.let { actionHeader ->
            // e.g. SOAPAction: example
            val soapAction = actionHeader.trim().removeSurrounding("\"").takeIf { it.isNotBlank() }
            LOGGER.trace("SOAPAction header: $soapAction")
            return soapAction
        }
        return null
    }

    private fun getSoapActionFromContentType(request: HttpRequest): String? {
        // e.g. application/soap+xml;charset=UTF-8;action="example"
        // for SOAP 1.2
        val contentTypeParts = request.getHeader("Content-Type")
            ?.split(";")
            ?.map { it.trim() }

        contentTypeParts?.let { headerParts ->
            if (headerParts.any { it == SoapUtil.soap12ContentType }) {
                val actionPart = headerParts.find { it.startsWith("action=") }
                val soapAction = actionPart?.removePrefix("action=")?.removeSurrounding("\"")
                LOGGER.trace("SOAPAction from content type header: $soapAction")
                return soapAction
            }
        }
        return null
    }

    private fun determineOperationFromRequestBody(bodyHolder: MessageBodyHolder): WsdlOperation? {
        val bodyRootElement = bodyHolder.bodyRootElement ?: run {
            LOGGER.warn("Missing body element in request")
            return null
        }

        val matchedOps = binding.operations.filter { op ->
            when (op.inputRef) {
                is ElementOperationMessage -> {
                    op.inputRef.elementName.namespaceURI == bodyRootElement.namespaceURI &&
                        op.inputRef.elementName.localPart == bodyRootElement.name
                }
                is TypeOperationMessage -> {
                    op.inputRef.typeName.namespaceURI == bodyRootElement.namespaceURI &&
                        op.inputRef.operationName == bodyRootElement.name
                }
                else -> false
            }
        }
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace(
                "Matched {} operations in binding {} based on body root element: {}: {}",
                matchedOps.size,
                binding.name,
                bodyRootElement.qualifiedName,
                matchedOps.map { it.name },
            )
        } else {
            LOGGER.debug(
                "Matched {} operations in binding {} based on body root element: {}",
                matchedOps.size,
                binding.name,
                bodyRootElement.qualifiedName,
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

    companion object {
        private val LOGGER = LogManager.getLogger(SoapResourceMatcher::class.java)
    }
}
