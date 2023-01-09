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

import com.google.common.base.Verify
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import io.gatehill.imposter.config.resolver.model.BodyPattern
import io.gatehill.imposter.config.resolver.model.WiremockMapping
import io.gatehill.imposter.config.resolver.model.WiremockMappings
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.plugin.config.resource.ResourceMatchOperator
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginResourceConfig
import io.gatehill.imposter.util.MapUtil
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

private typealias ExpressionHandler = (args: List<String>) -> String

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
                it.mappings.map { m -> convertMapping(sourceDir, localConfigDir, m) }
            }
            if (separateConfigFiles) {
                converted.forEachIndexed { index, res -> writeConfig(localConfigDir, index, res) }
            } else {
                writeConfig(localConfigDir, 0, converted.flatten())
            }
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

    private fun convertMapping(sourceDir: File, destDir: File, mapping: WiremockMapping) =
        RestPluginResourceConfig().apply {
            path = mapping.request.url
            method = mapping.request.method?.uppercase()?.let { HttpMethod.valueOf(it) }
            requestHeaders = mapping.request.headers?.let { convertConditionalHeaders(it) }
            requestBody = convertBodyPatterns(mapping.request.bodyPatterns)
            responseConfig.apply {
                statusCode = mapping.response.status
                headers = mapping.response.headers
                file = mapping.response.bodyFileName?.let {
                    convertResponseFile(sourceDir, destDir, mapping.response.bodyFileName)
                }
                isTemplate = mapping.response.transformers?.contains("response-template")
            }
        }

    /**
     * Example:
     * ```
     * [
     *   {
     *     "matchesXPath": {
     *       "expression": "//foo/text()",
     *       "contains": "bar"
     *      }
     *   },
     *   {
     *     "matchesXPath": {
     *       "expression": "//foo/text()",
     *       "equalTo": "baz"
     *      }
     *   },
     *   {
     *     "matchesXPath": {
     *       "expression": "//foo/text()",
     *       "matches": "f.*"
     *      }
     *   },
     *   {
     *     "matchesXPath": "/qux:corge",
     *     "xPathNamespaces": {
     *       "qux" : "http://example.com/somens"
     *     }
     *   }
     * ]
     * ```
     */
    private fun convertBodyPatterns(bodyPatterns: List<BodyPattern>?): RequestBodyConfig? {
        if (bodyPatterns?.isNotEmpty() == true) {
            if (bodyPatterns.size > 1) {
                logger.warn("Only one body pattern is supported - all but the first will be ignored")
            }
            val bodyPattern = bodyPatterns.first()
            val requestBodyConfig = RequestBodyConfig()
            when (bodyPattern.matchesXPath) {
                is String -> {
                    // body pattern using XPath with embedded conditional check, but no value
                    requestBodyConfig.xPath = bodyPattern.matchesXPath
                    requestBodyConfig.exists = true
                }

                is Map<*, *> -> {
                    bodyPattern.matchesXPath["expression"]?.let { expression ->
                        requestBodyConfig.xPath = "!$expression"
                    }
                    // operator
                    bodyPattern.matchesXPath["equalTo"]?.let { equalTo ->
                        requestBodyConfig.value = equalTo.toString()
                    } ?: bodyPattern.matchesXPath["contains"]?.let { contains ->
                        requestBodyConfig.value = contains.toString()
                        requestBodyConfig.operator = ResourceMatchOperator.Contains
                    } ?: bodyPattern.matchesXPath["matches"]?.let { matches ->
                        requestBodyConfig.value = matches.toString()
                        requestBodyConfig.operator = ResourceMatchOperator.Matches
                    } ?: bodyPattern.matchesXPath["doesNotMatch"]?.let { matches ->
                        requestBodyConfig.value = matches.toString()
                        requestBodyConfig.operator = ResourceMatchOperator.NotMatches
                    }
                }
            }
            requestBodyConfig.xmlNamespaces = bodyPattern.xPathNamespaces
            return requestBodyConfig

        } else {
            return null
        }
    }

    private fun convertConditionalHeaders(headers: Map<String, Map<String, String>>?) =
        headers?.mapNotNull { (k, v) -> v["equalTo"]?.let { k to it } }?.toMap()

    private fun convertResponseFile(
        sourceDir: File,
        destDir: File,
        bodyFileName: String
    ): String {
        val sourceFile = Paths.get(sourceDir.path, "__files", bodyFileName).normalize()
        if (!sourceFile.exists()) {
            throw FileNotFoundException("Response body file: $sourceFile does not exist")
        }
        val responseFile = convertPlaceholders(sourceFile.readText())

        val destFile = Paths.get(destDir.path, RESPONSE_FILE_SUBDIR, sourceFile.name)
        destFile.writeText(responseFile)
        logger.trace("Converted response file {} to {}", sourceFile, destFile)
        return RESPONSE_FILE_SUBDIR + "/" + destFile.name
    }

    private fun convertPlaceholders(source: String): String {
        val matcher = wiremockPlaceholderPattern.matcher(source)
        var matched = false
        val sb = StringBuffer()
        while (matcher.find()) {
            matched = true
            val args = matcher.group(2).split(whitespacePattern)
            val handler = expressionHandlers[matcher.group(1)]
            val result = handler?.invoke(args) ?: "null"
            matcher.appendReplacement(sb, result)
        }
        return if (matched) {
            matcher.appendTail(sb)
            sb.toString()
        } else {
            source
        }
    }

    companion object {
        private const val RESPONSE_FILE_SUBDIR = "files"

        /**
         * Examples:
         *
         *     {{jsonPath request.body '$.foo'}}
         *     {{xPath request.body '//foo'}}
         */
        private val wiremockPlaceholderPattern = Pattern.compile("\\{\\{\\s?(.+?)\\s+(.+?)\\s?}}")

        private val whitespacePattern = Pattern.compile("\\s+")

        private val expressionHandlers: Map<String, ExpressionHandler> = mapOf(
            "jsonPath" to parseSimple { input, expression -> "\\\${${input}:${expression}}" },
            "soapXPath" to parseSimple { input, expression -> "\\\${${input}:!/*[name()='Envelope']/*[name()='Body']${expression}}" },
            "xPath" to parseSimple { input, expression -> "\\\${${input}:${expression}}" },
            "randomValue" to parseRandom()
        )

        /**
         * Example:
         *
         *      xPath request.body '//foo/bar'
         */
        private fun parseSimple(handler: (input: String, expression: String) -> String): ExpressionHandler = { args ->
            val input = args[0].let { if (it.startsWith("request.")) "context.$it" else it }
            val expression = args[1].removeSurrounding("'")
            handler(input, expression)
        }

        /**
         * Example:
         *
         *     randomValue length=6 type='ALPHABETIC' uppercase=true
         */
        private fun parseRandom(): ExpressionHandler = { args ->
            var length: Int? = null
            var type: String? = null
            var uppercase = false
            for (arg in args) {
                if (arg.startsWith("length=")) {
                    length = arg.substringAfter("=").toInt()
                } else if (arg.startsWith("type=")) {
                    type = arg.substringAfter("=").removeSurrounding("'").lowercase()
                } else if (arg.startsWith("uppercase=")) {
                    uppercase = arg.substringAfter("=").toBoolean()
                }
            }
            Verify.verifyNotNull(type, "type is required")
            Verify.verifyNotNull(length, "length is required")
            "\\\${random.$type(length=$length,uppercase=$uppercase)}"
        }
    }
}
