/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.http

import com.google.common.base.Strings.isNullOrEmpty
import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.conditional.MatchOperator
import io.gatehill.imposter.plugin.config.resource.request.BaseRequestBodyConfig
import io.gatehill.imposter.plugin.config.resource.request.RequestBodyResourceConfig
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder
import io.gatehill.imposter.service.script.InlineScriptService
import io.gatehill.imposter.util.BodyQueryUtil
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.MatchUtil.safeEquals
import org.apache.logging.log4j.LogManager
import java.util.regex.Pattern


/**
 * Base class for matching resources using elements of the HTTP request.
 *
 * @author Pete Cornish
 */
abstract class AbstractResourceMatcher : ResourceMatcher {
    private val inlineScriptService: InlineScriptService by lazy { InjectorUtil.getInstance() }

    /**
     * {@inheritDoc}
     */
    override fun matchResourceConfig(
        pluginConfig: PluginConfig,
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): BasicResourceConfig? {
        val resourceConfigs = filterResourceConfigs(pluginConfig, resources, httpExchange)
        when (resourceConfigs.size) {
            0 -> {
                LOGGER.trace("No matching resource config for {}", LogUtil.describeRequestShort(httpExchange))
                return null
            }

            1 -> {
                LOGGER.debug("Matched resource config for {}", LogUtil.describeRequestShort(httpExchange))
                return resourceConfigs[0].resource.config
            }

            else -> {
                // multiple candidates - prefer exact matches
                val exactMatches = resourceConfigs.filter { it.exact }
                when (exactMatches.size) {
                    0 -> {
                        LOGGER.warn(
                            "More than one resource config matched a wildcard path for {} - this is probably a configuration error. Guessing first resource configuration.",
                            LogUtil.describeRequestShort(httpExchange)
                        )
                        return resourceConfigs[0].resource.config
                    }

                    1 -> {
                        LOGGER.debug("Matched resource config for {}", LogUtil.describeRequestShort(httpExchange))
                        return exactMatches[0].resource.config
                    }

                    else -> {
                        // find the most specific
                        val sorted = exactMatches.sortedByDescending { it.score }
                        if (sorted[0].score > sorted[1].score) {
                            LOGGER.debug("Matched resource config for {}", LogUtil.describeRequestShort(httpExchange))
                        } else {
                            LOGGER.warn(
                                "More than one resource config matched an exact path for {} - this is probably a configuration error. Guessing first resource configuration.",
                                LogUtil.describeRequestShort(httpExchange)
                            )
                        }
                        return sorted[0].resource.config
                    }
                }
            }
        }
    }

    private fun filterResourceConfigs(
        pluginConfig: PluginConfig,
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): List<MatchedResource> {
        return resources.map { matchRequest(pluginConfig, it, httpExchange) }.filter { it.matched }
    }

    /**
     * Determine if the resource configuration matches the current request.
     *
     * @param pluginConfig
     * @param resource     the resource configuration
     * @param httpExchange the current exchange
     * @return `true` if the resource matches the request, otherwise `false`
     */
    protected abstract fun matchRequest(
        pluginConfig: PluginConfig,
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource

    protected fun matchPath(
        httpExchange: HttpExchange,
        resourceConfig: BasicResourceConfig,
        request: HttpRequest,
    ): ResourceMatchResult {
        val matchDescription = "path"

        // note: path template can be null when a regex route is used
        val routePathTemplate = httpExchange.currentRoute?.path

        val pathMatch = resourceConfig.path?.let { resourceConfigPath ->
            if (resourceConfigPath.endsWith("*") && request.path.startsWith(resourceConfigPath.substring(0, resourceConfigPath.length - 1))) {
                return@let ResourceMatchResult.wildcardMatch(matchDescription)
            } else if (request.path == resourceConfigPath || routePathTemplate?.let { it == resourceConfigPath } == true) {
                return@let ResourceMatchResult.exactMatch(matchDescription)
            } else {
                return@let ResourceMatchResult.notMatched(matchDescription)
            }
            // path is un-set
        } ?: ResourceMatchResult.noConfig(matchDescription)

        return pathMatch
    }

    /**
     * Match the request body against the supplied configuration.
     *
     * @param httpExchange   thc current exchange
     * @param resourceConfig the match configuration
     * @return `true` if the configuration is empty, or the request body matches the configuration, otherwise `false`
     */
    protected fun matchRequestBody(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig
    ): ResourceMatchResult {
        val matchDescription = "request body"
        if (resourceConfig !is RequestBodyResourceConfig) {
            // none configured
            return ResourceMatchResult.noConfig(matchDescription)
        }

        resourceConfig.requestBody?.allOf?.let { bodyConfigs ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against all of ${bodyConfigs.size} request body configs for ${LogUtil.describeRequestShort(httpExchange)}: $bodyConfigs")
            }
            return if (bodyConfigs.all { matchUsingBodyConfig(matchDescription, it, pluginConfig, httpExchange).type == MatchResultType.EXACT_MATCH }) {
                // each matched config contributes to the weight
                ResourceMatchResult.exactMatch(matchDescription, bodyConfigs.size)
            } else {
                ResourceMatchResult.notMatched(matchDescription)
            }

        } ?: resourceConfig.requestBody?.anyOf?.let { bodyConfigs ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against any of ${bodyConfigs.size} request body configs for ${LogUtil.describeRequestShort(httpExchange)}: $bodyConfigs")
            }
            return if (bodyConfigs.any { matchUsingBodyConfig(matchDescription, it, pluginConfig, httpExchange).type == MatchResultType.EXACT_MATCH }) {
                ResourceMatchResult.exactMatch(matchDescription)
            } else {
                ResourceMatchResult.notMatched(matchDescription)
            }

        } ?: resourceConfig.requestBody?.let { singleRequestBodyConfig ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against a single request body config for ${LogUtil.describeRequestShort(httpExchange)}: $singleRequestBodyConfig")
            }
            return matchUsingBodyConfig(matchDescription, singleRequestBodyConfig, pluginConfig, httpExchange)

        } ?: run {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("No request body config to match for ${LogUtil.describeRequestShort(httpExchange)}")
            }
            // none configured
            return ResourceMatchResult.noConfig(matchDescription)
        }
    }

    private fun matchUsingBodyConfig(
            matchDescription: String,
            bodyConfig: BaseRequestBodyConfig,
            pluginConfig: PluginConfig,
            httpExchange: HttpExchange,
    ): ResourceMatchResult {
        return if (!isNullOrEmpty(bodyConfig.jsonPath)) {
            matchRequestBodyJsonPath(matchDescription, bodyConfig, httpExchange)
        } else if (!isNullOrEmpty(bodyConfig.xPath)) {
            matchRequestBodyXPath(matchDescription, bodyConfig, pluginConfig, httpExchange)
        } else {
            // none configured
            ResourceMatchResult.noConfig(matchDescription)
        }
    }

    private fun matchRequestBodyJsonPath(
            matchDescription: String,
            bodyConfig: BaseRequestBodyConfig,
            httpExchange: HttpExchange,
    ): ResourceMatchResult {
        val bodyValue = BodyQueryUtil.queryRequestBodyJsonPath(
            bodyConfig.jsonPath!!,
            httpExchange
        )
        // resource matching always uses strings
        return checkBodyMatch(matchDescription, bodyConfig, bodyValue?.toString())
    }

    private fun matchRequestBodyXPath(
            matchDescription: String,
            bodyConfig: BaseRequestBodyConfig,
            pluginConfig: PluginConfig,
            httpExchange: HttpExchange,
    ): ResourceMatchResult {
        val allNamespaces = bodyConfig.xmlNamespaces?.toMutableMap() ?: mutableMapOf()
        if (pluginConfig is SystemConfigHolder) {
            pluginConfig.systemConfig?.xmlNamespaces?.let { allNamespaces.putAll(it) }
        }
        val bodyValue = BodyQueryUtil.queryRequestBodyXPath(
            bodyConfig.xPath!!,
            allNamespaces,
            httpExchange
        )
        return checkBodyMatch(matchDescription, bodyConfig, bodyValue)
    }

    private fun checkBodyMatch(
            matchDescription: String,
            bodyConfig: BaseRequestBodyConfig,
            actualValue: Any?
    ): ResourceMatchResult {
        // defaults to equality check
        val operator = bodyConfig.operator ?: MatchOperator.EqualTo

        val match = when (operator) {
            MatchOperator.Exists, MatchOperator.NotExists ->
                matchIfExists(matchDescription, actualValue, operator)

            MatchOperator.EqualTo, MatchOperator.NotEqualTo ->
                matchUsingEquality(matchDescription, bodyConfig.value, actualValue, operator)

            MatchOperator.Contains, MatchOperator.NotContains ->
                matchUsingContains(matchDescription, bodyConfig.value, actualValue, operator)

            MatchOperator.Matches, MatchOperator.NotMatches ->
                matchUsingRegex(matchDescription, bodyConfig.value, actualValue, operator)
        }
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace("Body match result for {} '{}': {}", operator, bodyConfig.value, match)
        }
        return match
    }

    /**
     * The expression is checking for the existence of a value using the given query.
     */
    private fun matchIfExists(
            matchDescription: String,
            actualValue: Any?,
            operator: MatchOperator,
    ) = if ((actualValue != null) == (operator == MatchOperator.Exists)) {
        ResourceMatchResult.exactMatch(matchDescription)
    } else {
        ResourceMatchResult.notMatched(matchDescription)
    }

    private fun matchUsingEquality(
            matchDescription: String,
            configuredValue: String?,
            actualValue: Any?,
            operator: MatchOperator,
    ): ResourceMatchResult {
        val valueMatch = safeEquals(configuredValue, actualValue)

        // apply operator
        val match = (operator == MatchOperator.EqualTo && valueMatch ||
                operator == MatchOperator.NotEqualTo && !valueMatch)

        return if (match) {
            ResourceMatchResult.exactMatch(matchDescription)
        } else {
            ResourceMatchResult.notMatched(matchDescription)
        }
    }

    private fun matchUsingContains(
            matchDescription: String,
            configuredValue: String?,
            actualValue: Any?,
            operator: MatchOperator,
    ): ResourceMatchResult {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            actualValue.toString().contains(configuredValue)
        } else {
            false
        }

        // apply operator
        val match = (operator == MatchOperator.Contains && valueMatch ||
            operator == MatchOperator.NotContains && !valueMatch)

        return if (match) {
            ResourceMatchResult.exactMatch(matchDescription)
        } else {
            ResourceMatchResult.notMatched(matchDescription)
        }
    }

    private fun matchUsingRegex(
            matchDescription: String,
            configuredValue: String?,
            actualValue: Any?,
            operator: MatchOperator,
    ): ResourceMatchResult {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            val pattern = patternCache.get(configuredValue) { Pattern.compile(configuredValue) }
            pattern.matcher(actualValue.toString()).matches()
        } else {
            false
        }

        // apply operator
        val match = (operator == MatchOperator.Matches && valueMatch ||
            operator == MatchOperator.NotMatches && !valueMatch)

        return if (match) {
            ResourceMatchResult.exactMatch(matchDescription)
        } else {
            ResourceMatchResult.notMatched(matchDescription)
        }
    }

    protected fun matchEval(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resource: ResolvedResourceConfig,
    ) = inlineScriptService.evalScript(httpExchange, pluginConfig, resource.config)

    fun determineMatch(
        results: List<ResourceMatchResult>,
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource {
        // true if exact match or wildcard match, or partial config (implies match all)
        val matched = results.none { it.type == MatchResultType.NOT_MATCHED } &&
                !results.all { it.type == MatchResultType.NO_CONFIG }

        // all matched and none of type wildcard
        val exact = matched && results.none { it.type == MatchResultType.WILDCARD_MATCH }

        // score is the number of exact or wildcard matches
        val score = results.filter { it.type == MatchResultType.EXACT_MATCH || it.type == MatchResultType.WILDCARD_MATCH }
            .sumOf { it.weight }

        val outcome = MatchedResource(resource, matched, score, exact)
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace(
                "Request match evaluation for '{}' to resource {}, from results: {}, outcome: {}",
                LogUtil.describeRequest(httpExchange),
                resource.config,
                results,
                outcome,
            )
        }
        return outcome
    }

    data class MatchedResource(
        val resource: ResolvedResourceConfig,
        val matched: Boolean,
        val score: Int,
        val exact: Boolean,
    )

    companion object {
        private val LOGGER = LogManager.getLogger(AbstractResourceMatcher::class.java)
        private val patternCache = CacheBuilder.newBuilder().maximumSize(20).build<String, Pattern>()
    }
}
