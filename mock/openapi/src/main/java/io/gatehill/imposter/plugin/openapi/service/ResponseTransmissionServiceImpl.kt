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

import com.fasterxml.jackson.core.JsonProcessingException
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder
import io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.MapUtil.YAML_MAPPER
import io.swagger.v3.oas.models.examples.Example
import org.apache.logging.log4j.LogManager
import java.util.Objects

/**
 * Serialises and transmits examples to the client.
 *
 * @author Pete Cornish
 */
class ResponseTransmissionServiceImpl : ResponseTransmissionService {
    override fun <T> transmitExample(httpExchange: HttpExchange, example: ContentTypedHolder<T>) {
        val exampleValue: Any? = example.value
        if (Objects.isNull(exampleValue)) {
            LOGGER.info("No example found - returning empty response")
            httpExchange.response().end()
            return
        }
        val exampleResponse = buildExampleResponse(example.contentType, example.value)
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace(
                "Serving mock example for {} with status code {}: {}",
                LogUtil.describeRequestShort(httpExchange), httpExchange.response().getStatusCode(), exampleResponse
            )
        } else {
            LOGGER.info(
                "Serving mock example for {} with status code {} (response body {} bytes)",
                LogUtil.describeRequestShort(httpExchange), httpExchange.response().getStatusCode(),
                exampleResponse?.let { obj: String -> obj.length } ?: 0
            )
        }
        httpExchange.response()
            .putHeader(CONTENT_TYPE, example.contentType)
            .end(exampleResponse)
    }

    /**
     * Construct a response body from the example, based on the content type.
     *
     * @param contentType the content type
     * @param example     the example candidate - may be strongly typed [Example], map, list, or raw
     * @return the [String] representation of the example entry
     */
    private fun buildExampleResponse(contentType: String, example: Any?): String? {
        return when (example) {
            is Example -> {
                example.value?.toString()
            }
            is List<*> -> {
                serialiseList(contentType, example)
            }
            else -> (example as? Map<*, *>)?.let { serialise(contentType, it) }
                ?: if (example is String) {
                    example
                } else {
                    LOGGER.warn(
                        "Unsupported example type '{}' - attempting String conversion",
                        example?.javaClass
                    )
                    example?.toString()
                }
        }
    }

    /**
     * Serialises the list according to the content type.
     *
     * @param contentType the content type
     * @param example     a [List] to be serialised
     * @return the serialised list
     */
    private fun serialiseList(contentType: String, example: List<*>): String {
        val transformedList = transformListForSerialisation(example)
        return serialise(contentType, transformedList)
    }

    /**
     * Ensures each element can be serialised correctly as part of a list, allowing
     * for different list representations between serialisation formats.
     *
     * @param example the [List] whose elements to transform
     * @return the transformed list
     */
    private fun transformListForSerialisation(example: List<*>): List<*> {
        return example.map { e ->
            when (e) {
                is Example -> {
                    return@map e.value?.toString()
                }
                is List<*> -> {
                    return@map transformListForSerialisation(e)
                }
                is Map<*, *> -> {
                    return@map e
                }
                is String -> {
                    return@map e
                }
                else -> {
                    LOGGER.warn("Unsupported example element type '{}' - attempting String conversion", e?.javaClass)
                    return@map e?.toString()
                }
            }
        }
    }

    /**
     * Serialises the object according to the content type.
     *
     * @param contentType the content type
     * @param example     an object to be serialised
     * @return the serialisation
     */
    private fun serialise(contentType: String, example: Any): String {
        return try {
            val exampleResponse: String = when (contentType) {
                "application/json" -> MapUtil.jsonify(example)
                "text/x-yaml", "application/x-yaml", "application/yaml" -> YAML_MAPPER.writeValueAsString(example)
                else -> {
                    LOGGER.warn("Unsupported response MIME type '{}' - returning example object as string", contentType)
                    example.toString()
                }
            }
            exampleResponse
        } catch (e: JsonProcessingException) {
            LOGGER.error("Error building example response", e)
            ""
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ResponseTransmissionServiceImpl::class.java)
    }
}