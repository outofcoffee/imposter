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
package io.gatehill.imposter.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.MapperBuilder
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.gatehill.imposter.config.util.EnvVars
import org.yaml.snakeyaml.LoaderOptions

/**
 * @author Pete Cornish
 */
object MapUtil {
    @JvmField
    val JSON_MAPPER: ObjectMapper

    @JvmField
    val YAML_MAPPER: YAMLMapper

    /**
     * Don't apply standard configuration to this mapper.
     */
    val STATS_MAPPER: ObjectMapper = JsonMapper.builder().apply {
        if (EnvVars.getEnv("IMPOSTER_LOG_SUMMARY_PRETTY")?.toBoolean() == true) {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }.build()

    /**
     * The default code point limit is 3MB, which is too low for some YAML files.
     * See [Jackson documentation](https://github.com/FasterXML/jackson-dataformats-text/tree/2.15/yaml#maximum-input-yaml-document-size-3-mb)
     */
    private val yamlCodePointLimit: Int by lazy {
        EnvVars.getEnv("IMPOSTER_YAML_CODE_POINT_LIMIT")?.toInt() ?: (3 * 1024 * 1024)
    }

    init {
        JSON_MAPPER = configureBuilder<ObjectMapper>(
                JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT)
        ).apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

        val yamlBuilder = YAMLMapper.builder(YAMLFactory.builder()
                .loaderOptions(LoaderOptions().apply { codePointLimit = yamlCodePointLimit })
                .build())
        YAML_MAPPER = configureBuilder(yamlBuilder)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <M : ObjectMapper> configureBuilder(builder: MapperBuilder<*, *>): M {
        val mapper = builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()

        addJavaTimeSupport(mapper)
        mapper.registerKotlinModule()

        return mapper as M
    }

    /**
     * Adds support for JSR-310 data types
     *
     * @param mapper the [@ObjectMapper] to modify
     */
    fun addJavaTimeSupport(mapper: ObjectMapper) {
        mapper.registerModule(JavaTimeModule())
    }

    fun jsonify(obj: Any?): String = obj?.let { JSON_MAPPER.writeValueAsString(it) } ?: ""

    fun yamlify(obj: Any?): String = obj?.let { YAML_MAPPER.writeValueAsString(obj) } ?: ""
}
