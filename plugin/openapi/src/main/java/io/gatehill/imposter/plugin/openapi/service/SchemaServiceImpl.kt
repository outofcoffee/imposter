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

import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder
import io.gatehill.imposter.plugin.openapi.service.valueprovider.DEFAULT_VALUE_PROVIDERS
import io.gatehill.imposter.plugin.openapi.util.RefUtil
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.vertx.core.http.HttpServerRequest
import org.apache.logging.log4j.LogManager
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Collects examples from schema definitions.
 *
 * @author benjvoigt
 * @author Pete Cornish
 */
class SchemaServiceImpl : SchemaService {
    override fun collectExamples(
        request: HttpServerRequest,
        spec: OpenAPI,
        schema: ContentTypedHolder<Schema<*>>
    ): ContentTypedHolder<*> {
        val example = collectSchemaExample(spec, schema.value)
        LOGGER.trace(
            "Collected example from {} schema for {} {}: {}",
            schema.contentType,
            request.method(),
            request.absoluteURI(),
            example
        )
        return ContentTypedHolder(schema.contentType, example)
    }

    private fun collectSchemaExample(spec: OpenAPI, schema: Schema<*>): Any? {
        // $ref takes precedence, per spec:
        //   "Any sibling elements of a $ref are ignored. This is because
        //   $ref works by replacing itself and everything on its level
        //   with the definition it is pointing at."
        // See: https://swagger.io/docs/specification/using-ref/
        val example: Any? = if (Objects.nonNull(schema.`$ref`)) {
            val referent = RefUtil.lookupSchemaRef(spec, schema)
            collectSchemaExample(spec, referent)
        } else if (Objects.nonNull(schema.example)) {
            if (schema is DateTimeSchema) {
                DATE_TIME_FORMATTER.format(schema.getExample() as OffsetDateTime)
            } else if (schema is DateSchema) {
                DATE_FORMATTER.format((schema.getExample() as Date).toInstant())
            } else {
                schema.example
            }
        } else if (Objects.nonNull(schema.properties)) {
            buildFromProperties(spec, schema.properties)
        } else if (ObjectSchema::class.java.isAssignableFrom(schema.javaClass)) {
            val objectSchema = schema as ObjectSchema?
            buildFromProperties(spec, objectSchema!!.properties)
        } else if (ArraySchema::class.java.isAssignableFrom(schema.javaClass)) {
            buildFromArraySchema(spec, schema as ArraySchema?)
        } else if (ComposedSchema::class.java.isAssignableFrom(schema.javaClass)) {
            buildFromComposedSchema(spec, schema as ComposedSchema?)
        } else {
            getPropertyDefault(schema)
        }
        return example
    }

    private fun buildFromArraySchema(spec: OpenAPI, schema: ArraySchema?): List<Any?> {
        // items may be a schema type with multiple children
        val items = schema!!.items
        val examples: MutableList<Any?> = mutableListOf()
        examples.add(collectSchemaExample(spec, items))
        return examples
    }

    private fun buildFromComposedSchema(spec: OpenAPI, schema: ComposedSchema?): Any? {
        val example: Any? = if (Objects.nonNull(schema!!.allOf) && schema.allOf.isNotEmpty()) {
            val allOf = schema.allOf

            // Combine properties of 'allOf'
            // See: https://swagger.io/docs/specification/data-models/oneof-anyof-allof-not/
            val combinedExampleProperties: MutableMap<String, Any> = mutableMapOf()
            allOf.forEach { s ->
                val exampleMap = collectSchemaExample(spec, s)
                if (Objects.nonNull(exampleMap) && exampleMap is Map<*, *>) {
                    // FIXME code defensively around this cast
                    @Suppress("UNCHECKED_CAST")
                    combinedExampleProperties.putAll((exampleMap as Map<String, Any>?)!!)
                }
            }
            combinedExampleProperties

        } else if (Objects.nonNull(schema.oneOf) && schema.oneOf.isNotEmpty()) {
            LOGGER.debug(
                "Found 'oneOf' in schema {} - using first schema example", (schema.name ?: "")
            )
            val oneOf = schema.oneOf
            collectSchemaExample(spec, oneOf[0])

        } else if (Objects.nonNull(schema.anyOf) && schema.anyOf.isNotEmpty()) {
            LOGGER.debug(
                "Found 'anyOf' in schema {} - using first schema example", (schema.name ?: "")
            )
            val anyOf = schema.anyOf
            collectSchemaExample(spec, anyOf[0])

        } else if (Objects.nonNull(schema.not)) {
            LOGGER.debug(
                "Found 'not' in schema {} - using null for schema example", (schema.name ?: "")
            )
            null

        } else {
            throw IllegalStateException("Invalid composed schema; missing or empty [allOf, oneOf, anyOf]")
        }
        return example
    }

    private fun buildFromProperties(spec: OpenAPI, properties: Map<String, Schema<*>>): Map<String, Any?> {
        return if (Objects.isNull(properties)) {
            emptyMap<String, Any>()
        } else {
            properties.entries.associate { (k, v) -> k to collectSchemaExample(spec, v) }
        }
    }

    private fun getPropertyDefault(schema: Schema<*>?): Any? {
        // if a non-empty enum exists, choose the first value
        if (Objects.nonNull(schema!!.enum) && !schema.enum.isEmpty()) {
            return schema.enum[0]
        }

        // fall back to a default for the type
        if (Objects.nonNull(schema.type)) {
            val defaultValueProvider = DEFAULT_VALUE_PROVIDERS[schema.type]!!
            return if (Objects.nonNull(defaultValueProvider)) {
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
        private val LOGGER = LogManager.getLogger(
            SchemaServiceImpl::class.java
        )
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.from(ZoneOffset.UTC))
        val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))
    }
}