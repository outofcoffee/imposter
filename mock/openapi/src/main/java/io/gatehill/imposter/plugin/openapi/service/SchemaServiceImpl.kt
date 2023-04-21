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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder
import io.gatehill.imposter.plugin.openapi.service.valueprovider.DEFAULT_VALUE_PROVIDERS
import io.gatehill.imposter.plugin.openapi.util.RefUtil
import io.gatehill.imposter.util.DateTimeUtil
import io.gatehill.imposter.util.LogUtil
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import org.apache.logging.log4j.LogManager
import java.time.OffsetDateTime
import java.util.*
import java.util.Objects.nonNull

/**
 * Collects examples from schema definitions.
 *
 * @author benjvoigt
 * @author Pete Cornish
 */
class SchemaServiceImpl : SchemaService {
    override fun collectExamples(
            httpExchange: HttpExchange,
            spec: OpenAPI,
            schema: ContentTypedHolder<Schema<*>>
    ): ContentTypedHolder<*> {
        val example = collectSchemaExample(spec, schema.value)
        LOGGER.trace(
                "Collected example from {} schema for {}: {}",
                schema.contentType,
                LogUtil.describeRequestShort(httpExchange),
                example
        )
        return ContentTypedHolder(schema.contentType, example)
    }

    private fun collectSchemaExample(spec: OpenAPI, schema: Schema<*>): Any? {
        try {
            // $ref takes precedence, per spec:
            //   "Any sibling elements of a $ref are ignored. This is because
            //   $ref works by replacing itself and everything on its level
            //   with the definition it is pointing at."
            // See: https://swagger.io/docs/specification/using-ref/
            val example: Any? = if (nonNull(schema.`$ref`)) {
                val referent = RefUtil.lookupSchemaRef(spec, schema)
                collectSchemaExample(spec, referent)
            } else if (nonNull(schema.example)) {
                when (schema) {
                    is DateTimeSchema -> {
                        DateTimeUtil.DATE_TIME_FORMATTER.format(schema.getExample() as OffsetDateTime)
                    }

                    is DateSchema -> {
                        DateTimeUtil.DATE_FORMATTER.format((schema.getExample() as Date).toInstant())
                    }

                    else -> {
                        schema.example
                    }
                }
            } else if (nonNull(schema.properties)) {
                buildFromProperties(spec, schema.properties)
            } else {
                when (schema) {
                    is ObjectSchema -> buildFromProperties(spec, schema.properties)
                    is ArraySchema -> buildFromArraySchema(spec, schema)
                    is ComposedSchema -> buildFromComposedSchema(spec, schema)
                    else -> getPropertyDefault(schema)
                }
            }
            return example
        } catch (e: Exception) {
            throw IllegalStateException("Failed to collect example from schema: $schema", e)
        }
    }

    private fun buildFromArraySchema(spec: OpenAPI, schema: ArraySchema): List<Any?> {
        if (null == schema.items) {
            return emptyList()
        }
        // items may be a schema type with multiple children
        return mutableListOf(
                collectSchemaExample(spec, schema.items)
        )
    }

    private fun buildFromComposedSchema(spec: OpenAPI, schema: ComposedSchema): Any? {
        val example: Any? = if (nonNull(schema.allOf) && schema.allOf.isNotEmpty()) {
            val allOf = schema.allOf

            // Combine properties of 'allOf'
            // See: https://swagger.io/docs/specification/data-models/oneof-anyof-allof-not/
            val combinedExampleProperties: MutableMap<String, Any> = mutableMapOf()
            allOf.forEach { s ->
                val exampleMap = collectSchemaExample(spec, s)
                if (nonNull(exampleMap) && exampleMap is Map<*, *>) {
                    // FIXME code defensively around this cast
                    @Suppress("UNCHECKED_CAST")
                    combinedExampleProperties.putAll((exampleMap as Map<String, Any>?)!!)
                }
            }
            combinedExampleProperties

        } else if (nonNull(schema.oneOf) && schema.oneOf.isNotEmpty()) {
            LOGGER.debug(
                    "Found 'oneOf' in schema {} - using first schema example", (schema.name ?: "")
            )
            val oneOf = schema.oneOf
            collectSchemaExample(spec, oneOf[0])

        } else if (nonNull(schema.anyOf) && schema.anyOf.isNotEmpty()) {
            LOGGER.debug(
                    "Found 'anyOf' in schema {} - using first schema example", (schema.name ?: "")
            )
            val anyOf = schema.anyOf
            collectSchemaExample(spec, anyOf[0])

        } else if (nonNull(schema.not)) {
            LOGGER.debug(
                    "Found 'not' in schema {} - using null for schema example", (schema.name ?: "")
            )
            null

        } else {
            throw IllegalStateException("Invalid composed schema; missing or empty [allOf, oneOf, anyOf]")
        }
        return example
    }

    /**
     * @param properties must be nullable as the return type of [Schema.properties] can be `null`
     */
    private fun buildFromProperties(
            spec: OpenAPI,
            properties: Map<String, Schema<*>>?
    ): Map<String, Any?> {
        return properties?.entries?.associate { (k, v) ->
            k to collectSchemaExample(spec, v)
        } ?: emptyMap<String, Any>()
    }

    private fun getPropertyDefault(schema: Schema<*>?): Any? {
        // if a non-empty enum exists, choose the first value
        if (nonNull(schema!!.enum) && schema.enum.isNotEmpty()) {
            return schema.enum[0]
        }

        // fall back to a default for the type
        if (nonNull(schema.type)) {
            val defaultValueProvider = DEFAULT_VALUE_PROVIDERS[schema.type]!!
            return if (nonNull(defaultValueProvider)) {
                defaultValueProvider.provide(schema)
            } else {
                LOGGER.warn(
                        "Unknown type: {} for schema: {} - returning null for example property",
                        schema.type,
                        schema.name
                )
                null
            }
        }
        LOGGER.warn("Missing type for schema: {} - returning null for example property", schema.name)
        return null
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SchemaServiceImpl::class.java)
    }
}