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
import io.gatehill.imposter.plugin.config.resource.ResourceMatchOperator
import io.gatehill.imposter.plugin.config.resource.reqbody.BaseRequestBodyConfig
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder
import io.gatehill.imposter.service.script.InlineScriptService
import io.gatehill.imposter.util.BodyQueryUtil
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.StringUtil.safeEquals
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
        // note: path template can be null when a regex route is used
        val routePathTemplate = httpExchange.currentRoute?.path

        val pathMatch = resourceConfig.path?.let { resourceConfigPath ->
            if (resourceConfigPath.endsWith("*") && request.path.startsWith(resourceConfigPath.substring(0, resourceConfigPath.length - 1))) {
                return@let ResourceMatchResult.WILDCARD_MATCH
            } else if (request.path == resourceConfigPath || routePathTemplate?.let { it == resourceConfigPath } == true) {
                return@let ResourceMatchResult.EXACT_MATCH
            } else {
                return@let ResourceMatchResult.NOT_MATCHED
            }
            // path is un-set
        } ?: ResourceMatchResult.NO_CONFIG

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
        if (resourceConfig !is RequestBodyResourceConfig) {
            // none configured
            return ResourceMatchResult.NO_CONFIG
        }
        return resourceConfig.requestBody?.allOf?.let { bodyConfigs ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against all of ${bodyConfigs.size} request body configs for ${LogUtil.describeRequestShort(httpExchange)}: $bodyConfigs")
            }
            if (bodyConfigs.all { matchUsingBodyConfig(it, pluginConfig, httpExchange) == ResourceMatchResult.EXACT_MATCH }) {
                // each matched config contributes to the weight
                ResourceMatchResult(MatchResultType.EXACT_MATCH, bodyConfigs.size)
            } else {
                ResourceMatchResult.NOT_MATCHED
            }

        } ?: resourceConfig.requestBody?.anyOf?.let { bodyConfigs ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against any of ${bodyConfigs.size} request body configs for ${LogUtil.describeRequestShort(httpExchange)}: $bodyConfigs")
            }
            if (bodyConfigs.any { matchUsingBodyConfig(it, pluginConfig, httpExchange) == ResourceMatchResult.EXACT_MATCH }) {
                ResourceMatchResult.EXACT_MATCH
            } else {
                ResourceMatchResult.NOT_MATCHED
            }

        } ?: resourceConfig.requestBody?.let { singleRequestBodyConfig ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against a single request body config for ${LogUtil.describeRequestShort(httpExchange)}: $singleRequestBodyConfig")
            }
            matchUsingBodyConfig(singleRequestBodyConfig, pluginConfig, httpExchange)

        } ?: run {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("No request body config to match for ${LogUtil.describeRequestShort(httpExchange)}")
            }
            // none configured
            ResourceMatchResult.NO_CONFIG
        }
    }

    private fun matchUsingBodyConfig(
        bodyConfig: BaseRequestBodyConfig,
        pluginConfig: PluginConfig,
        httpExchange: HttpExchange,
    ): ResourceMatchResult {
        return if (!isNullOrEmpty(bodyConfig.jsonPath)) {
            matchRequestBodyJsonPath(bodyConfig, httpExchange)
        } else if (!isNullOrEmpty(bodyConfig.xPath)) {
            matchRequestBodyXPath(bodyConfig, pluginConfig, httpExchange)
        } else {
            // none configured
            ResourceMatchResult.NO_CONFIG
        }
    }

    private fun matchRequestBodyJsonPath(
        bodyConfig: BaseRequestBodyConfig,
        httpExchange: HttpExchange,
    ): ResourceMatchResult {
        val bodyValue = BodyQueryUtil.queryRequestBodyJsonPath(
            bodyConfig.jsonPath!!,
            httpExchange
        )
        // resource matching always uses strings
        return checkBodyMatch(bodyConfig, bodyValue?.toString())
    }

    private fun matchRequestBodyXPath(
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
        return checkBodyMatch(bodyConfig, bodyValue)
    }

    private fun checkBodyMatch(bodyConfig: BaseRequestBodyConfig, actualValue: Any?): ResourceMatchResult {
        // defaults to equality check
        val operator = bodyConfig.operator ?: ResourceMatchOperator.EqualTo

        val match = when (operator) {
            ResourceMatchOperator.Exists, ResourceMatchOperator.NotExists ->
                matchIfExists(actualValue, operator)

            ResourceMatchOperator.EqualTo, ResourceMatchOperator.NotEqualTo ->
                matchUsingEquality(bodyConfig.value, actualValue, operator)

            ResourceMatchOperator.Contains, ResourceMatchOperator.NotContains ->
                matchUsingContains(bodyConfig.value, actualValue, operator)

            ResourceMatchOperator.Matches, ResourceMatchOperator.NotMatches ->
                matchUsingRegex(bodyConfig.value, actualValue, operator)
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
        actualValue: Any?,
        operator: ResourceMatchOperator,
    ) = if ((actualValue != null) == (operator == ResourceMatchOperator.Exists)) {
        ResourceMatchResult.EXACT_MATCH
    } else {
        ResourceMatchResult.NOT_MATCHED
    }

    private fun matchUsingEquality(
        configuredValue: String?,
        actualValue: Any?,
        operator: ResourceMatchOperator,
    ): ResourceMatchResult {
        val valueMatch = safeEquals(configuredValue, actualValue)

        // apply operator
        val match = (operator === ResourceMatchOperator.EqualTo && valueMatch ||
                operator === ResourceMatchOperator.NotEqualTo && !valueMatch)

        return if (match) ResourceMatchResult.EXACT_MATCH else ResourceMatchResult.NOT_MATCHED
    }

    private fun matchUsingContains(
        configuredValue: String?,
        actualValue: Any?,
        operator: ResourceMatchOperator,
    ): ResourceMatchResult {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            actualValue.toString().contains(configuredValue)
        } else {
            false
        }

        // apply operator
        val match = (operator === ResourceMatchOperator.Contains && valueMatch ||
            operator === ResourceMatchOperator.NotContains && !valueMatch)

        return if (match) ResourceMatchResult.EXACT_MATCH else ResourceMatchResult.NOT_MATCHED
    }

    private fun matchUsingRegex(
        configuredValue: String?,
        actualValue: Any?,
        operator: ResourceMatchOperator,
    ): ResourceMatchResult {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            val pattern = patternCache.get(configuredValue) { Pattern.compile(configuredValue) }
            pattern.matcher(actualValue.toString()).matches()
        } else {
            false
        }

        // apply operator
        val match = (operator === ResourceMatchOperator.Matches && valueMatch ||
            operator === ResourceMatchOperator.NotMatches && !valueMatch)

        return if (match) ResourceMatchResult.EXACT_MATCH else ResourceMatchResult.NOT_MATCHED
    }

    protected fun matchEval(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resource: ResolvedResourceConfig,
    ) = inlineScriptService.evalScript(httpExchange, pluginConfig, resource.config)

    protected fun determineMatch(
        results: List<ResourceMatchResult>,
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource {
        // true if exact match, wildcard match, or no config (implies match all)
        val matched = results.none { it == ResourceMatchResult.NOT_MATCHED }

        // all matched and none of type wildcard
        val exact = matched && results.none { it == ResourceMatchResult.WILDCARD_MATCH }

        // score is the number of exact or wildcard matches
        val score = results.filter { it == ResourceMatchResult.EXACT_MATCH || it == ResourceMatchResult.WILDCARD_MATCH }
            .sumOf { it.weight }

        val result = MatchedResource(resource, matched, score, exact)
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace("Result of matching request {} to resource {}: {}", LogUtil.describeRequest(httpExchange), resource.config, result)
        }
        return result
    }

    protected data class MatchedResource(
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
