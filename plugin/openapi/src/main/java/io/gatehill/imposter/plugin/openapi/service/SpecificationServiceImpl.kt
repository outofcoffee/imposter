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

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import com.fasterxml.jackson.core.JsonGenerationException
import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginValidationConfig.ValidationIssueBehaviour
import io.gatehill.imposter.plugin.openapi.util.ValidationReportUtil
import io.gatehill.imposter.util.MapUtil
import io.swagger.models.Scheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Function

/**
 * @author Pete Cornish
 */
class SpecificationServiceImpl : SpecificationService {
    private val cache = CacheBuilder.newBuilder().build<String, Any>()
    private val reportFormatter = SimpleValidationReportFormat.getInstance()

    @Throws(ExecutionException::class)
    override fun getCombinedSpec(imposterConfig: ImposterConfig, allSpecs: List<OpenAPI>?): OpenAPI {
        return cache.get("combinedSpecObject") {
            try {
                val scheme = Scheme.forValue(imposterConfig.pluginArgs!![ARG_SCHEME])
                val basePath = imposterConfig.pluginArgs!![ARG_BASEPATH]
                val title = imposterConfig.pluginArgs!![ARG_TITLE]
                return@get combineSpecifications(allSpecs, basePath, scheme, title)
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        } as OpenAPI
    }

    @Throws(ExecutionException::class)
    override fun getCombinedSpecSerialised(imposterConfig: ImposterConfig, allSpecs: List<OpenAPI>?): String {
        return cache.get("combinedSpecSerialised") {
            try {
                return@get MapUtil.JSON_MAPPER.writeValueAsString(getCombinedSpec(imposterConfig, allSpecs))
            } catch (e: JsonGenerationException) {
                throw ExecutionException(e)
            }
        } as String
    }

    override fun combineSpecifications(
        specs: List<OpenAPI>?,
        basePath: String?,
        scheme: Scheme?,
        title: String?
    ): OpenAPI {
        Objects.requireNonNull(specs, "Input specifications must not be null")
        LOGGER.debug("Generating combined specification from {} inputs", specs!!.size)

        val combined = OpenAPI()
        val info = Info()
        combined.info = info

        val servers: MutableList<Server> = mutableListOf()
        combined.servers = servers

        val security: MutableList<SecurityRequirement> = mutableListOf()
        combined.security = security

        val paths = Paths()
        combined.paths = paths

        val tags: MutableList<Tag> = mutableListOf()
        combined.tags = tags

        val allExternalDocs: MutableList<ExternalDocumentation> = mutableListOf()
        val allComponents: MutableList<Components> = mutableListOf()
        val description = StringBuilder().append("This specification includes the following APIs:")

        specs.forEach { spec: OpenAPI ->
            spec.info?.let { specInfo: Info ->
                description
                    .append("\n* **")
                    .append(specInfo.title)
                    .append("**")
                    .append(specInfo.description?.let { specDesc -> " - $specDesc" } ?: "")
            }
            servers.addAll(spec.servers ?: emptyList())
            security.addAll(spec.security ?: emptyList())
            tags.addAll(spec.tags ?: emptyList())
            if (Objects.nonNull(spec.externalDocs)) {
                allExternalDocs.add(spec.externalDocs)
            }
            if (Objects.nonNull(spec.components)) {
                allComponents.add(spec.components)
            }
            paths.putAll(spec.paths ?: emptyMap())
        }

        // info
        info.title = title ?: DEFAULT_TITLE
        info.description = description.toString()

        // external docs
        val externalDocs = ExternalDocumentation()
        externalDocs.description = allExternalDocs
            .filter { it.description != null }
            .joinToString(System.lineSeparator()) { it.description }

        // NOTE: The OAS spec only permits a single URL, so, to avoid confusion,
        // we don't set it at all.
        combined.externalDocs = externalDocs

        // components
        val components = Components()
        components.callbacks = aggregate(allComponents) { obj: Components -> obj.callbacks }
        components.examples = aggregate(allComponents) { obj: Components -> obj.examples }
        components.extensions = aggregate(allComponents) { obj: Components -> obj.extensions }
        components.headers = aggregate(allComponents) { obj: Components -> obj.headers }
        components.links = aggregate(allComponents) { obj: Components -> obj.links }
        components.parameters = aggregate(allComponents) { obj: Components -> obj.parameters }
        components.requestBodies = aggregate(allComponents) { obj: Components -> obj.requestBodies }
        components.responses = aggregate(allComponents) { obj: Components -> obj.responses }
        components.schemas = aggregate(allComponents) { obj: Components -> obj.schemas }
        components.securitySchemes = aggregate(allComponents) { obj: Components -> obj.securitySchemes }
        combined.components = components
        setServers(combined, servers, scheme, basePath)
        combined.paths = paths
        combined.security = security
        combined.tags = tags
        return combined
    }

    override fun isValidRequest(
        imposterConfig: ImposterConfig,
        pluginConfig: OpenApiPluginConfig,
        routingContext: RoutingContext,
        allSpecs: List<OpenAPI>?
    ): Boolean {
        if (Objects.isNull(pluginConfig.validation)) {
            LOGGER.trace("Validation is disabled")
            return true
        }
        if (ValidationIssueBehaviour.IGNORE == pluginConfig.validation?.request) {
            LOGGER.trace("Request validation is disabled")
            return true
        }
        if (ValidationIssueBehaviour.IGNORE != pluginConfig.validation?.response) {
            throw UnsupportedOperationException("Response validation is not supported")
        }

        val validator: OpenApiInteractionValidator = try {
            getValidator(imposterConfig, pluginConfig, allSpecs)
        } catch (e: ExecutionException) {
            routingContext.fail(RuntimeException("Error building spec validator", e))
            return false
        }

        val request = routingContext.request()
        val requestBuilder = SimpleRequest.Builder(request.method().toString(), request.path())
            .withBody(routingContext.bodyAsString)

        routingContext.queryParams().forEach { p ->
            requestBuilder.withQueryParam(p.key, p.value)
        }
        request.headers().forEach { h ->
            requestBuilder.withHeader(h.key, h.value)
        }

        val report = validator.validateRequest(requestBuilder.build())
        if (report.messages.isNotEmpty()) {
            val reportMessages = reportFormatter.apply(report)
            LOGGER.warn("Validation failed for {} {}: {}", request.method(), request.absoluteURI(), reportMessages)

            // only respond with 400 if validation failures are at error level
            if (report.hasErrors() && ValidationIssueBehaviour.FAIL == pluginConfig.validation.request) {
                val response = routingContext.response()
                response.statusCode = 400
                if (pluginConfig.validation.returnErrorsInResponse) {
                    ValidationReportUtil.sendValidationReport(routingContext, reportMessages)
                } else {
                    response.end()
                }
                return false
            }
        } else {
            LOGGER.debug("Validation passed for {} {}", request.method(), request.absoluteURI())
        }
        return true
    }

    /**
     * Returns the specification validator from cache, creating it first on cache miss.
     */
    @Throws(ExecutionException::class)
    private fun getValidator(
        imposterConfig: ImposterConfig,
        pluginConfig: OpenApiPluginConfig,
        allSpecs: List<OpenAPI>?
    ): OpenApiInteractionValidator {
        return cache.get("specValidator") {
            val combined = getCombinedSpec(imposterConfig, allSpecs)
            val builder = OpenApiInteractionValidator.createFor(combined)

            // custom validation levels
            pluginConfig.validation?.levels?.let { levels ->
                LOGGER.trace("Using custom validation levels: {}", levels)
                val levelBuilder = LevelResolver.create()
                (defaultValidationLevels + levels).forEach { (key, value) ->
                    levelBuilder.withLevel(key, ValidationReport.Level.valueOf(value))
                }
                builder.withLevelResolver(levelBuilder.build())
            }
            builder.build()
        } as OpenApiInteractionValidator
    }

    private fun setServers(combined: OpenAPI, servers: MutableList<Server>, scheme: Scheme?, basePath: String?) {
        scheme?.let {
            servers.forEach { server: Server ->
                overrideScheme(scheme.toValue(), server)
            }
        }
        basePath?.let {
            if (servers.isEmpty()) {
                val server = Server()
                servers.add(server)
            }
            servers.forEach { server: Server -> prefixPath(basePath, server) }
        }
        combined.servers = servers.distinct()
    }

    /**
     * Override the scheme of the [Server] URL.
     *
     * @param requiredScheme the scheme to set
     * @param server         the server to modify
     */
    private fun overrideScheme(requiredScheme: String, server: Server) {
        try {
            val original = URI(server.url)
            if (Objects.nonNull(original.scheme) && !original.scheme.equals(requiredScheme, ignoreCase = true)) {
                val modified = URI(
                    requiredScheme,
                    original.userInfo,
                    original.host,
                    original.port,
                    original.path,
                    original.query,
                    original.fragment
                )
                server.url = modified.toASCIIString()
            }
        } catch (ignored: URISyntaxException) {
        } catch (e: Exception) {
            LOGGER.warn("Error overriding scheme to '{}' for server URL: {}", requiredScheme, server.url)
        }
    }

    /**
     * Prefix the path of the [Server] URL.
     *
     * @param basePath the path prefix
     * @param server   the server to modify
     */
    private fun prefixPath(basePath: String, server: Server) {
        try {
            val original = URI(server.url)
            val modified = URI(
                original.scheme,
                original.userInfo,
                original.host,
                original.port,
                basePath + (original.path ?: ""),
                original.query,
                original.fragment
            )
            server.url = modified.toASCIIString()
        } catch (ignored: URISyntaxException) {
        } catch (e: Exception) {
            LOGGER.warn("Error prefixing scheme with '{}' for server URL: {}", basePath, server.url)
        }
    }

    private fun <H, T> aggregate(allHolders: List<H>, mapSupplier: Function<H, Map<String, T>>): Map<String, T> {
        val all: MutableMap<String, T> = mutableMapOf()
        allHolders.stream()
            .map(mapSupplier)
            .filter { obj: Map<String, T>? -> Objects.nonNull(obj) }
            .forEach { m: Map<String, T>? ->
                all.putAll(m!!)
            }
        return all
    }

    companion object {
        private val LOGGER = LogManager.getLogger(
            SpecificationServiceImpl::class.java
        )
        private const val DEFAULT_TITLE = "Imposter Mock APIs"
        private const val ARG_BASEPATH = "openapi.basepath"
        private const val ARG_SCHEME = "openapi.scheme"
        private const val ARG_TITLE = "openapi.title"

        private val defaultValidationLevels = mapOf(
            "validation.request.parameter.query.unexpected" to "IGNORE"
        )
    }
}