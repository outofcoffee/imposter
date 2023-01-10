/*
 * Copyright (c) 2023.
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
package io.gatehill.imposter.config.resolver

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import io.gatehill.imposter.config.resolver.model.WiremockMapping
import io.gatehill.imposter.config.resolver.model.WiremockMappings
import io.gatehill.imposter.config.resolver.util.ConversionUtil
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.plugin.rest.config.RestPluginConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginResourceConfig
import io.gatehill.imposter.util.MapUtil
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Converts wiremock mappings to Imposter format in a temporary directory,
 * then returns the path to the temporary directory.
 */
class WiremockConfigResolver : ConfigResolver {
    private val logger = LogManager.getLogger(WiremockConfigResolver::class.java)
    private val separateConfigFiles = EnvVars.getEnv("IMPOSTER_WIREMOCK_SEPARATE_CONFIG").toBoolean()

    private val mappingCache = CacheBuilder.newBuilder().maximumSize(20)
        .build(object : CacheLoader<String, List<WiremockMappings>>() {
            override fun load(key: String) = loadMappings(key)
        })

    override fun handles(configPath: String): Boolean {
        val wiremockMappings = mappingCache.get(configPath)
        return wiremockMappings?.isNotEmpty() ?: false
    }

    override fun resolve(configPath: String): File {
        val sourceDir = File(configPath)
        val localConfigDir = Files.createTempDirectory("wiremock").toFile()

        val responseFileDir = File(localConfigDir, RESPONSE_FILE_SUBDIR)
        if (!responseFileDir.mkdirs()) {
            throw IOException("Failed to create response file dir: $responseFileDir")
        }

        mappingCache.get(configPath)?.let { wiremockMappings ->
            logger.debug("Converting ${wiremockMappings.size} wiremock mapping file(s) from $configPath")
            val converted = wiremockMappings.map {
                it.mappings.mapNotNull { m -> convertMapping(sourceDir, localConfigDir, m) }
            }
            if (separateConfigFiles) {
                converted.forEachIndexed { index, res -> writeConfig(localConfigDir, index, res) }
            } else {
                writeConfig(localConfigDir, 0, converted.flatten())
            }
            logger.debug("Wrote converted wiremock mapping file(s) to $localConfigDir")

        } ?: run {
            logger.warn("No wiremock mapping files found in $configPath")
        }
        return localConfigDir
    }

    private fun loadMappings(configPath: String): List<WiremockMappings> =
        File(configPath, "mappings").listFiles { _, filename -> filename.endsWith(".json") }?.mapNotNull { jsonFile ->
            try {
                val config = MapUtil.JSON_MAPPER.readValue(jsonFile, WiremockMappings::class.java)
                logger.trace("Parsed {} as wiremock mapping file: {}", configPath, config)
                config
            } catch (e: Exception) {
                logger.trace("Unable to parse {} as wiremock mapping file: {}", configPath, e.message)
                null
            }
        } ?: emptyList()

    private fun writeConfig(destDir: File, index: Int, resources: List<RestPluginResourceConfig>) {
        val destFile = File(destDir, "wiremock-$index-config.json")
        val config = RestPluginConfig().apply {
            plugin = "rest"
            this.resources = resources
        }
        destFile.outputStream().use { os ->
            MapUtil.JSON_MAPPER.writeValue(os, config)
            os.flush()
        }
        logger.trace("Converted wiremock mapping file to Imposter config: {}", destFile)
    }

    private fun convertMapping(sourceDir: File, destDir: File, mapping: WiremockMapping): RestPluginResourceConfig? {
        if (null == mapping.request.url) {
            logger.warn("Skipping conversion of mapping with no URL: $mapping")
            return null
        }

        val uri = URI(mapping.request.url)
        return RestPluginResourceConfig().apply {
            path = uri.path
            queryParams = ConversionUtil.convertQueryParams(uri.query)
            method = mapping.request.method?.uppercase()?.let { HttpMethod.valueOf(it) }
            requestHeaders = mapping.request.headers?.let { ConversionUtil.convertHeaders(it) }
            requestBody = ConversionUtil.convertBodyPatterns(mapping.request.bodyPatterns)
            responseConfig.apply {
                statusCode = mapping.response.status
                headers = mapping.response.headers
                file = mapping.response.bodyFileName?.let {
                    convertResponseFile(sourceDir, destDir, mapping.response.bodyFileName)
                }
                // TODO consider whether to move inline content to response files
                content = mapping.response.jsonBody?.let {
                    headers = (headers?.toMutableMap() ?: mutableMapOf()).apply {
                        this["content-type"] = "application/json"
                    }
                    return@let MapUtil.JSON_MAPPER.writeValueAsString(it)
                }
                isTemplate = mapping.response.transformers?.contains("response-template")
            }
        }
    }

    private fun convertResponseFile(
        sourceDir: File,
        destDir: File,
        bodyFileName: String
    ): String {
        val sourceFile = Paths.get(sourceDir.path, "__files", bodyFileName).normalize()
        if (!sourceFile.exists()) {
            throw FileNotFoundException("Response body file: $sourceFile does not exist")
        }
        val responseFile = ConversionUtil.convertPlaceholders(sourceFile.readText())

        val destFile = Paths.get(destDir.path, RESPONSE_FILE_SUBDIR, sourceFile.name)
        destFile.writeText(responseFile)
        logger.trace("Converted response file {} to {}", sourceFile, destFile)
        return RESPONSE_FILE_SUBDIR + "/" + destFile.name
    }

    companion object {
        private const val RESPONSE_FILE_SUBDIR = "files"
    }
}
