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
package io.gatehill.imposter.plugin.wiremock

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.config.resource.ResponseConfig
import io.gatehill.imposter.plugin.rest.RestPluginImpl
import io.gatehill.imposter.plugin.rest.config.RestPluginConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginResourceConfig
import io.gatehill.imposter.plugin.wiremock.model.WiremockFile
import io.gatehill.imposter.plugin.wiremock.model.WiremockMapping
import io.gatehill.imposter.plugin.wiremock.util.ConversionUtil
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.MapUtil
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

@PluginInfo("wiremock")
class WiremockPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    resourceService: ResourceService,
    responseService: ResponseService,
    responseRoutingService: ResponseRoutingService,
) : RestPluginImpl(
    vertx,
    imposterConfig,
    resourceService,
    responseService,
    responseRoutingService,
) {
    private val logger = LogManager.getLogger(WiremockPluginImpl::class.java)
    private val separateConfigFiles = EnvVars.getEnv("IMPOSTER_WIREMOCK_SEPARATE_CONFIG").toBoolean()

    override fun loadConfiguration(configFiles: List<File>) {
        super.loadConfiguration(configFiles.flatMap { convert(it) })
    }

    /**
     * Converts wiremock mappings to Imposter format in a temporary directory,
     * then returns the path to the generated config file.
     */
    internal fun convert(mappingsFile: File): List<File> {
        val sourceDir = mappingsFile.parentFile
        val localConfigDir = Files.createTempDirectory("wiremock").toFile()

        val mappings = loadMappings(sourceDir)
        if (!mappings.isNullOrEmpty()) {
            logger.debug("Converting ${mappings.size} wiremock mapping file(s) from $sourceDir")

            val responseFileDir = File(localConfigDir, RESPONSE_FILE_SUBDIR)
            if (!responseFileDir.mkdirs()) {
                throw IOException("Failed to create response file dir: $responseFileDir")
            }

            val configFiles = mutableListOf<File>()
            val converted = mappings.map { mf ->
                mf.mapNotNull { m -> convertMapping(sourceDir, localConfigDir, m) }
            }
            if (converted.isNotEmpty()) {
                if (separateConfigFiles) {
                    configFiles += converted.mapIndexed { index, res -> writeConfig(localConfigDir, index, res) }
                } else {
                    configFiles += writeConfig(localConfigDir, 0, converted.flatten())
                }
                logger.debug("Wrote converted wiremock mapping file(s) to $localConfigDir")
                return configFiles
            }
        }
        logger.warn("No wiremock mapping files found in $sourceDir")
        return emptyList()
    }

    private fun loadMappings(configPath: File): List<List<WiremockMapping>>? =
        File(configPath, "mappings").listFiles { _, filename -> filename.endsWith(".json") }?.mapNotNull { jsonFile ->
            try {
                val config = MapUtil.JSON_MAPPER.readValue(jsonFile, WiremockFile::class.java)
                logger.trace("Parsed {} as wiremock mapping file: {}", configPath, config)
                if (config.mappings.isNullOrEmpty()) {
                    return@mapNotNull listOf(WiremockMapping(config.request!!, config.response!!))
                } else {
                    return@mapNotNull config.mappings
                }
            } catch (e: Exception) {
                logger.trace("Unable to parse {} as wiremock mapping file: {}", configPath, e.message)
                null
            }
        }

    private fun writeConfig(destDir: File, index: Int, resources: List<RestPluginResourceConfig>): File {
        val destFile = File(destDir, "wiremock-$index-config.json")
        val config = RestPluginConfig().apply {
            plugin = "rest"
            this.resources = resources
        }
        destFile.writeText(MapUtil.jsonify(config))
        logger.trace("Converted wiremock mapping file to Imposter config: {}", destFile)
        return destFile
    }

    private fun convertMapping(sourceDir: File, destDir: File, mapping: WiremockMapping): RestPluginResourceConfig? {
        val url = mapping.request.url
        if (null == url) {
            logger.warn("Skipping conversion of mapping with no URL: $mapping")
            return null
        }

        val uri = URI(url)
        return RestPluginResourceConfig().apply {
            path = uri.path
            queryParams = ConversionUtil.convertQueryParams(uri.query)
            method = mapping.request.method?.uppercase()?.let { HttpMethod.valueOf(it) }
            requestHeaders = mapping.request.headers?.let { ConversionUtil.convertHeaders(it) }
            requestBody = ConversionUtil.convertBodyPatterns(mapping.request.bodyPatterns)
            responseConfig.apply {
                statusCode = mapping.response.status
                headers = mapping.response.headers
                file = mapping.response.bodyFileName?.let { bodyFileName ->
                    convertResponseFile(sourceDir, destDir, bodyFileName)
                }
                // TODO consider moving inline content to response files
                content = mapping.response.body ?: mapping.response.jsonBody?.let { jsonBody ->
                    convertJsonBody(responseConfig, jsonBody)
                }
                isTemplate = mapping.response.transformers?.contains("response-template")
                failureType = ConversionUtil.convertFault(mapping.response.fault)
                performanceDelay = ConversionUtil.convertDelay(mapping.response)
            }
        }
    }

    private fun convertResponseFile(
        sourceDir: File,
        destDir: File,
        bodyFileName: String,
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

    private fun convertJsonBody(responseConfig: ResponseConfig, it: Any): String {
        responseConfig.setHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
        return MapUtil.jsonify(it)
    }

    companion object {
        private const val RESPONSE_FILE_SUBDIR = "files"
    }
}
