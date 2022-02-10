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

package io.gatehill.imposter.plugin.soap

import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.http.AbstractResourceMatcher
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.plugin.soap.config.SoapPluginResourceConfig
import io.gatehill.imposter.plugin.soap.model.ParsedSoapMessage
import io.gatehill.imposter.plugin.soap.model.WsdlBinding
import io.gatehill.imposter.plugin.soap.model.WsdlOperation
import io.gatehill.imposter.plugin.soap.util.SoapUtil
import org.apache.logging.log4j.LogManager

/**
 * SOAP specific matcher, for a particular binding.
 *
 * @author Pete Cornish
 */
class SoapResourceMatcher(
    private val binding: WsdlBinding,
) : AbstractResourceMatcher() {

    /**
     * {@inheritDoc}
     */
    override fun isRequestMatch(
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): Boolean {
        val resourceConfig = resource.config as SoapPluginResourceConfig
        val request = httpExchange.request()

        val pathMatch = isPathMatch(httpExchange, resourceConfig, request)
        val bindingMatch = resourceConfig.binding?.let { it == binding.name } ?: true

        val soapAction = getSoapAction(httpExchange)
        val soapActionMatch = resourceConfig.soapAction?.let { it == soapAction } ?: true

        val operationMatch = resourceConfig.operation?.let { isOperationMatch(httpExchange, it, soapAction) } ?: true

        return pathMatch && bindingMatch && operationMatch && soapActionMatch &&
            matchRequestBody(httpExchange, resource.config)
    }

    private fun isPathMatch(
        httpExchange: HttpExchange,
        resourceConfig: SoapPluginResourceConfig,
        request: HttpRequest
    ): Boolean {
        // note: path template can be null when a regex route is used
        val pathTemplate = httpExchange.currentRoutePath

        // if path is un-set, implies match all
        val pathMatch = resourceConfig.path?.let {
            request.path() == resourceConfig.path || (pathTemplate?.let { it == resourceConfig.path } == true)
        } ?: true

        return pathMatch
    }

    private fun isOperationMatch(httpExchange: HttpExchange, configOpName: String, soapAction: String?) = httpExchange.body?.let { body ->
        val soapEnv = SoapUtil.parseSoapEnvelope(body)
        val operation = determineOperation(soapAction, soapEnv)
        configOpName == operation?.name
    } ?: false

    fun determineOperation(soapAction: String?, soapEnv: ParsedSoapMessage): WsdlOperation? {
        soapAction?.let {
            return binding.operations.firstOrNull { it.soapAction == soapAction }
        }
        return determineOperationFromRequestBody(soapEnv)
    }

    fun getSoapAction(httpExchange: HttpExchange): String? {
        val request = httpExchange.request()

        val soapAction: String? = getSoapActionHeader(request)
            ?: getSoapActionFromContentType(request)

        soapAction ?: LOGGER.trace("No SOAPAction found")
        return soapAction
    }

    private fun getSoapActionHeader(request: HttpRequest) :String? {
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

    private fun determineOperationFromRequestBody(soapEnv: ParsedSoapMessage): WsdlOperation? {
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
