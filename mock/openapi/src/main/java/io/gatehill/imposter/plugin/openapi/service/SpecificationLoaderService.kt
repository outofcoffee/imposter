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

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatehill.imposter.config.S3FileDownloader
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.service.FileCacheService
import io.gatehill.imposter.util.MapUtil.JSON_MAPPER
import io.gatehill.imposter.util.MapUtil.YAML_MAPPER
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import io.swagger.v3.parser.util.RemoteUrl
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.readBytes

/**
 * Utility functions to load the OpenAPI specification, determining the version and use the appropriate parser.
 *
 * @author Pete Cornish
 */
class SpecificationLoaderService @Inject constructor(
    private val fileCacheService: FileCacheService
) {
    private val logger = LogManager.getLogger(SpecificationLoaderService::class.java)

    private val useFileCacheForRemoteSpecs: Boolean =
        EnvVars.getEnv("IMPOSTER_OPENAPI_REMOTE_FILE_CACHE")?.toBoolean() == true

    fun parseSpecification(config: OpenApiPluginConfig): OpenAPI {
        val specFile = config.specFile ?: throw IllegalStateException("No specification file configured")
        val specData: String = loadSpecFromSourceOrCache(specFile, config)

        // determine serialisation
        val parsed: Map<*, *> = try {
            val mapper = determineMapper(specFile, specData)
            mapper.readValue(specData, HashMap::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Error preparsing specification: $specFile", e)
        }

        // determine version
        val specVersion = determineVersion(specFile, parsed)
        logger.trace("Using version: {} parser for: {}", specVersion, specFile)

        val parseOptions = ParseOptions()
        parseOptions.setResolveFully(true)

        // convert or parse directly
        val parseResult: SwaggerParseResult? = when (specVersion) {
            SpecVersion.V2 -> SwaggerConverter().readContents(specData, emptyList(), parseOptions)
            SpecVersion.V3 -> OpenAPIV3Parser().readContents(specData, emptyList(), parseOptions)
        }
        checkNotNull(parseResult) { "Unable to parse specification: $specFile" }

        if (parseResult.messages?.isNotEmpty() == true) {
            logger.info(
                "OpenAPI parser messages for: {}: {}",
                specFile,
                java.lang.String.join(System.lineSeparator(), parseResult.messages)
            )
        }
        checkNotNull(parseResult.openAPI) { "Unable to parse specification: $specFile" }

        return parseResult.openAPI
    }

    /**
     * If [useFileCacheForRemoteSpecs] is `true`, attempt to retrieve [specFile] from the
     * file cache, falling back to retrieval from source.
     */
    private fun loadSpecFromSourceOrCache(specFile: String, config: OpenApiPluginConfig): String {
        try {
            val cacheEnabled = useFileCacheForRemoteSpecs && isRemoteLocation(specFile)

            if (cacheEnabled) {
                val cacheResult = fileCacheService.readFromCache(specFile)
                if (cacheResult.hit) {
                    val specContent = cacheResult.value!!
                    logger.debug("Specification $specFile read [${specContent.size} bytes] from cache")
                    return String(specContent)
                } else {
                    logger.trace("File cache miss for spec: $specFile - falling back to source")
                }
            }

            val specData = loadSpecFromSource(config, specFile)
            if (cacheEnabled) {
                fileCacheService.writeToCache(specFile, specData)
            }
            return specData

        } catch (e: Exception) {
            throw RuntimeException("Failed to load specification from source [$specFile] or cache", e)
        }
    }

    private fun isRemoteLocation(specFile: String): Boolean =
        specFile.startsWith("http://")
                || specFile.startsWith("https://")
                || specFile.startsWith("s3://")

    private fun loadSpecFromSource(config: OpenApiPluginConfig, specFile: String): String {
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
            logger.debug("Specification read [{} bytes] from URL: {}", specData.length, specUrl)
            specData
        } catch (e: Exception) {
            throw RuntimeException("Error fetching remote specification from: $specUrl", e)
        }
    }

    private fun readSpecFromFile(config: OpenApiPluginConfig, specFile: String): String {
        val specPath = Paths.get(config.parentDir.absolutePath, specFile)
        return try {
            String(specPath.readBytes())
        } catch (e: IOException) {
            throw RuntimeException("Error reading specification: $specPath", e)
        }
    }

    private fun determineMapper(specFile: String, specData: String): ObjectMapper {
        logger.trace("Determining serialisation for: {}", specFile)
        return if (specData.trim { it <= ' ' }.startsWith("{")) {
            JSON_MAPPER
        } else {
            YAML_MAPPER
        }
    }

    private fun determineVersion(specFile: String, parsed: Map<*, *>): SpecVersion {
        logger.trace("Determining version for: {}", specFile)
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
            logger.warn("Could not determine version for: {} - guessing V3", specFile)
            SpecVersion.V3
        }
    }

    private enum class SpecVersion {
        V2, V3
    }
}
