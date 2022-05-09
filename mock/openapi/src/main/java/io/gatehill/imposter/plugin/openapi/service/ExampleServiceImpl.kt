/*
 * Copyright (c) 2016-2021.
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
package io.gatehill.imposter.plugin.openapi.service

import com.google.common.base.Strings
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder
import io.gatehill.imposter.plugin.openapi.util.RefUtil
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.util.HttpUtil.readAcceptedContentTypes
import io.gatehill.imposter.util.LogUtil
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.apache.logging.log4j.LogManager
import java.util.*
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ExampleServiceImpl @Inject constructor(
    private val schemaService: SchemaService,
    private val responseTransmissionService: ResponseTransmissionService
) : ExampleService {

    /**
     * {@inheritDoc}
     */
    override fun serveExample(
        imposterConfig: ImposterConfig,
        config: OpenApiPluginConfig,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        specResponse: ApiResponse,
        spec: OpenAPI
    ): Boolean {
        return findContent(spec, specResponse)?.let { responseContent ->
            findInlineExample(config, httpExchange, responseBehaviour, responseContent)?.let { inlineExample ->
                responseTransmissionService.transmitExample(httpExchange, inlineExample)
                true

            } ?: run {
                LOGGER.trace("No inline examples found; checking schema")
                findResponseSchema(config, httpExchange, responseContent)?.let { schema ->
                    return@run serveFromSchema(httpExchange, spec, schema)
                }
            } ?: false

        } ?: run {
            LOGGER.debug(
                "No matching examples found in specification for {} and status code {}",
                LogUtil.describeRequestShort(httpExchange), responseBehaviour.statusCode
            )

            // no matching example
            return false
        }
    }

    private fun findContent(spec: OpenAPI, response: ApiResponse): Content? {
        // $ref takes precedence, per spec:
        //   "Any sibling elements of a $ref are ignored. This is because
        //   $ref works by replacing itself and everything on its level
        //   with the definition it is pointing at."
        // See: https://swagger.io/docs/specification/using-ref/
        return if (Objects.nonNull(response.`$ref`)) {
            LOGGER.trace("Using response from component reference: {}", response.`$ref`)
            val resolvedResponse = RefUtil.lookupResponseRef(spec, response)
            resolvedResponse.content
        } else if (Objects.nonNull(response.content)) {
            LOGGER.trace("Using inline response")
            response.content
        } else {
            null
        }
    }

    private fun findInlineExample(
        config: OpenApiPluginConfig,
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        responseContent: Content
    ): ContentTypedHolder<Any>? {
        val examples: MutableList<ResponseEntities<Any>> = mutableListOf()

        // fetch all examples
        responseContent.forEach { mimeTypeName: String, mediaType: MediaType ->
            // Example field takes precedence, per spec:
            //  "The example field is mutually exclusive of the examples field."
            // See: https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#mediaTypeObject
            if (Objects.nonNull(mediaType.example)) {
                examples.add(ResponseEntities.of("inline example", mimeTypeName, mediaType.example))
            } else if (Objects.nonNull(mediaType.examples)) {
                mediaType.examples.forEach { (exampleName: String, example: Example) ->
                    examples.add(
                        ResponseEntities.of(
                            "inline example",
                            mimeTypeName,
                            example,
                            exampleName
                        )
                    )
                }
            }
        }
        val example: ContentTypedHolder<Any>? = if (examples.size > 0) {
            LOGGER.trace(
                "Checking for mock example in specification ({} candidates) for {}",
                examples.size, LogUtil.describeRequestShort(httpExchange)
            )
            matchExample(httpExchange, config, responseBehaviour, examples)
        } else {
            null
        }
        return example
    }

    private fun findResponseSchema(
        config: OpenApiPluginConfig,
        httpExchange: HttpExchange,
        responseContent: Content
    ): ContentTypedHolder<Schema<*>>? {
        val schemas: MutableList<ResponseEntities<Schema<*>>> = mutableListOf()
        responseContent.forEach { mimeTypeName: String, mediaType: MediaType ->
            if (Objects.nonNull(mediaType.schema)) {
                schemas.add(ResponseEntities.of("response schema", mimeTypeName, mediaType.schema))
            }
        }
        return matchByContentType(httpExchange, config, schemas)
    }

    /**
     * Locate an item of type [T], first by searching the matched examples by name,
     * then by content type, then, optionally, falling back to the first found.
     *
     * @param httpExchange    the HTTP exchange
     * @param config            the plugin configuration
     * @param responseBehaviour the response behaviour
     * @param entriesToSearch   the examples
     * @return an optional, containing the object for the given content type
     */
    private fun <T> matchExample(
        httpExchange: HttpExchange,
        config: OpenApiPluginConfig,
        responseBehaviour: ResponseBehaviour,
        entriesToSearch: List<ResponseEntities<T>>
    ): ContentTypedHolder<T>? {
        // a specific example has been selected
        if (!Strings.isNullOrEmpty(responseBehaviour.exampleName)) {
            entriesToSearch.firstOrNull { responseBehaviour.exampleName == it.name }?.let { responseEntities ->
                LOGGER.debug("Exact example selected: {}", responseBehaviour.exampleName)
                return convertToContentTypedExample(responseEntities)
            } ?: LOGGER.warn("No example named '{}' was present", responseBehaviour.exampleName)
        }
        return matchByContentType(httpExchange, config, entriesToSearch)
    }

    /**
     * Locate an entity of type [T], first by searching the matched content types, then, optionally,
     * falling back to the first found.
     *
     * @param httpExchange  the HTTP exchange
     * @param config          the plugin configuration
     * @param entriesToSearch the examples
     * @return an optional, containing the object for the given content type
     */
    private fun <T> matchByContentType(
        httpExchange: HttpExchange,
        config: OpenApiPluginConfig,
        entriesToSearch: List<ResponseEntities<T>>
    ): ContentTypedHolder<T>? {
        // the produced content types
        val produces = entriesToSearch.map { entry: ResponseEntities<T> -> entry.contentType }.distinct()

        // match accepted content types to those produced by this response operation
        val matchedContentTypes = readAcceptedContentTypes(httpExchange).filter { produces.contains(it) }

        // match example by produced and accepted content types, assuming example name is a content type
        if (matchedContentTypes.isNotEmpty()) {
            val candidateEntities = entriesToSearch.filter { example: ResponseEntities<T> ->
                matchedContentTypes.contains(example.contentType)
            }

            if (candidateEntities.isNotEmpty()) {
                val entity = candidateEntities[0]
                if (candidateEntities.size > 1) {
                    LOGGER.warn(
                        "More than one {} found matching accepted content types ({}), but no example name specified. Selecting first entry: {}",
                        entity.entityDescription,
                        matchedContentTypes,
                        entity.contentType
                    )
                } else {
                    LOGGER.debug(
                        "Exact {} match found for accepted content type ({}) from specification",
                        entity.entityDescription,
                        entity.contentType
                    )
                }
                return convertToContentTypedExample(entity)
            }
        }

        // fallback to first example found
        if (config.isPickFirstIfNoneMatch && entriesToSearch.isNotEmpty()) {
            val example = entriesToSearch.iterator().next()
            LOGGER.debug(
                "No exact match found for accepted content types - choosing first item found ({}) from specification." +
                        " You can switch off this behaviour by setting configuration option 'pickFirstIfNoneMatch: false'",
                example.contentType
            )
            return convertToContentTypedExample(example)
        }

        // no matching example
        return null
    }

    private fun serveFromSchema(
        httpExchange: HttpExchange,
        spec: OpenAPI,
        schema: ContentTypedHolder<Schema<*>>
    ): Boolean {
        return try {
            val dynamicExamples = schemaService.collectExamples(httpExchange, spec, schema)
            responseTransmissionService.transmitExample(httpExchange, dynamicExamples)
            true
        } catch (e: Exception) {
            LOGGER.error("Error serving example from schema", e)
            false
        }
    }

    private class ResponseEntities<T> private constructor(
        val entityDescription: String,
        val contentType: String,
        val item: T,
        val name: String?
    ) {
        companion object {
            fun <T> of(entityDescription: String, contentType: String, item: T): ResponseEntities<T> {
                return of(entityDescription, contentType, item, null)
            }

            fun <T> of(entityDescription: String, contentType: String, item: T, name: String?): ResponseEntities<T> {
                return ResponseEntities(entityDescription, contentType, item, name)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ExampleServiceImpl::class.java)

        private fun <T> convertToContentTypedExample(entry: ResponseEntities<T>): ContentTypedHolder<T> {
            return ContentTypedHolder(entry.contentType, entry.item)
        }
    }
}