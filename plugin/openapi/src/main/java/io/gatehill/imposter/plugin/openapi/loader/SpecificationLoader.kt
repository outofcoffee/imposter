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
package io.gatehill.imposter.plugin.openapi.loader

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatehill.imposter.config.S3FileDownloader
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.util.MapUtil.JSON_MAPPER
import io.gatehill.imposter.util.MapUtil.YAML_MAPPER
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import io.swagger.v3.parser.util.RemoteUrl
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * Utility functions to load the OpenAPI specification, determining the version and use the appropriate parser.
 *
 * @author Pete Cornish
 */
object SpecificationLoader {
    private val LOGGER = LogManager.getLogger(SpecificationLoader::class.java)

    @JvmStatic
    fun parseSpecification(config: OpenApiPluginConfig): OpenAPI {
        val specFile = config.specFile ?: throw IllegalStateException("No specification file configured")
        val specData = loadSpecData(config, specFile)

        // determine serialisation
        val parsed: Map<*, *> = try {
            val mapper = determineMapper(specFile, specData)
            mapper.readValue(specData, HashMap::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Error preparsing specification: $specFile", e)
        }

        // determine version
        val specVersion = determineVersion(specFile, parsed)
        LOGGER.trace("Using version: {} parser for: {}", specVersion, specFile)

        val parseOptions = ParseOptions()
        parseOptions.isResolveFully = true

        // convert or parse directly
        val parseResult: SwaggerParseResult? = when (specVersion) {
            SpecVersion.V2 -> SwaggerConverter().readContents(specData, emptyList(), parseOptions)
            SpecVersion.V3 -> OpenAPIV3Parser().readContents(specData, emptyList(), parseOptions)
        }
        checkNotNull(parseResult) { "Unable to parse specification: $specFile" }

        if (null != parseResult.messages && parseResult.messages.isNotEmpty()) {
            LOGGER.info(
                "OpenAPI parser messages for: {}: {}",
                specFile, java.lang.String.join(System.lineSeparator(), parseResult.messages)
            )
        }
        checkNotNull(parseResult.openAPI) { "Unable to parse specification: $specFile" }

        return parseResult.openAPI
    }

    private fun loadSpecData(config: OpenApiPluginConfig, specFile: String): String {
        return if (specFile.startsWith("http://") || specFile.startsWith("https://")) {
            readSpecFromUrl(specFile)
        } else if (specFile.startsWith("s3://")) {
            S3FileDownloader.getInstance().readFileFromS3(specFile)
        } else {
            readSpecFromFile(config, specFile)
        }
    }

    private fun readSpecFromUrl(specUrl: String): String {
        return try {
            val specData = RemoteUrl.urlToString(specUrl, emptyList())
            LOGGER.debug("Specification read [{} bytes] from URL: {}", specData.length, specUrl)
            specData
        } catch (e: Exception) {
            throw RuntimeException("Error fetching remote specification from: $specUrl", e)
        }
    }

    private fun readSpecFromFile(config: OpenApiPluginConfig, specFile: String): String {
        val specPath = Paths.get(config.parentDir.absolutePath, specFile)
        return try {
            FileUtils.readFileToString(specPath.toFile(), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw RuntimeException("Error reading specification: $specPath", e)
        }
    }

    private fun determineMapper(specFile: String, specData: String): ObjectMapper {
        LOGGER.trace("Determining serialisation for: {}", specFile)
        return if (specData.trim { it <= ' ' }.startsWith("{")) {
            JSON_MAPPER
        } else {
            YAML_MAPPER
        }
    }

    private fun determineVersion(specFile: String, parsed: Map<*, *>): SpecVersion {
        LOGGER.trace("Determining version for: {}", specFile)
        val versionString = parsed["openapi"] as String?
            ?: parsed["swagger"] as String?
            ?: ""

        return if (versionString == "3" || versionString.startsWith("3.")) {
            // OpenAPI v3
            SpecVersion.V3
        } else if (versionString == "2" || versionString.startsWith("2.")) {
            // Swagger/OpenAPI v2
            SpecVersion.V2
        } else {
            // default to v3
            LOGGER.warn("Could not determine version for: {} - guessing V3", specFile)
            SpecVersion.V3
        }
    }

    private enum class SpecVersion {
        V2, V3
    }
}
