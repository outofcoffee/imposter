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

package io.gatehill.imposter.util

import com.google.common.base.Strings
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ParseContext
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.gatehill.imposter.http.HttpExchange
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.xpath.XPathExpression
import org.jdom2.xpath.XPathFactory
import java.io.StringReader
import java.util.concurrent.atomic.AtomicReference

/**
 * Convenience wrapper for JsonPath and XPath queries.
 *
 * @author Pete Cornish
 */
object BodyQueryUtil {
    private val logger : Logger = LogManager.getLogger(BodyQueryUtil::class.java)

    val JSONPATH_PARSE_CONTEXT: ParseContext = JsonPath.using(
        Configuration.builder()
            .mappingProvider(JacksonMappingProvider())
            .build()
    )

    fun buildXPath(expression: String, xPathNamespaces: Map<String, String> = emptyMap()): XPathExpression<*> {
        return buildXPath(expression, buildNamespaces(xPathNamespaces))
    }

    fun buildXPath(expression: String, xPathNamespaces: List<Namespace> = emptyList()): XPathExpression<*> {
        return XPathFactory.instance().compile(expression, Filters.element(), emptyMap(), xPathNamespaces)
    }

    private fun buildNamespaces(namespaces: Map<String, String>?) =
        namespaces?.map { (prefix, uri) -> Namespace.getNamespace(prefix, uri) } ?: emptyList()

    fun selectSingleNode(context: Any, expression: String, xPathNamespaces: List<Namespace>): Element? {
        val xPath = buildXPath(expression, xPathNamespaces)
        return xPath.evaluateFirst(context) as Element?
    }

    @Suppress("UNCHECKED_CAST")
    fun selectNodes(context: Any, expression: String, xPathNamespaces: List<Namespace>): List<Element> {
        val xPath = buildXPath(expression, xPathNamespaces)
        return xPath.evaluate(context) as List<Element>
    }

    fun queryRequestBodyJsonPath(
        jsonPath: String,
        httpExchange: HttpExchange
    ): Any? {
        val body = httpExchange.request().bodyAsString
        return if (Strings.isNullOrEmpty(body)) {
            null
        } else {
            try {
                val jsonPathContext = getRequestJsonContext(httpExchange, body)
                jsonPathContext.read<Any>(jsonPath)
            } catch (ignored: PathNotFoundException) {
                // this is just a negative result
                null
            } catch (e: Exception) {
                logger.warn("Error evaluating JsonPath expression '$jsonPath' against request body for ${LogUtil.describeRequest(httpExchange)}", e)
                null
            }
        }
    }

    /**
     * Gets the JSON document context for JsonPath queries against the request body.
     * The context is cached in the [HttpExchange].
     */
    fun getRequestJsonContext(httpExchange: HttpExchange, body: String? = httpExchange.request().bodyAsString): DocumentContext {
        val jsonContextHolder =
            httpExchange.getOrPut("request.json.context", { AtomicReference<DocumentContext>() })

        var jsonContext = jsonContextHolder.get()
        if (null == jsonContext) {
            jsonContext = JSONPATH_PARSE_CONTEXT.parse(body)
            jsonContextHolder.set(jsonContext)
        }
        return jsonContext
    }

    fun queryRequestBodyXPath(
        xPath: String,
        xmlNamespaces: Map<String, String>?,
        httpExchange: HttpExchange
    ): Any? {
        val body = httpExchange.request().bodyAsString
        return if (Strings.isNullOrEmpty(body)) {
            null
        } else {
            try {
                val document = getRequestXmlDocument(httpExchange, body)
                selectSingleNode(document, xPath, buildNamespaces(xmlNamespaces))?.value
            } catch (e: Exception) {
                logger.warn("Error evaluating XPath expression '$xPath' against request body for ${LogUtil.describeRequest(httpExchange)}", e)
                null
            }
        }
    }

    /**
     * Gets the XML document for XPath queries against the request body.
     * The document is cached in the [HttpExchange].
     */
    private fun getRequestXmlDocument(httpExchange: HttpExchange, body: String?): Document {
        val xmlDocumentHolder =
            httpExchange.getOrPut("request.xml.document", { AtomicReference<Document>() })

        var xmlDocument = xmlDocumentHolder.get()
        if (null == xmlDocument) {
            xmlDocument = SAXBuilder().build(StringReader(body ?: ""))
            xmlDocumentHolder.set(xmlDocument)
        }
        return xmlDocument
    }
}
