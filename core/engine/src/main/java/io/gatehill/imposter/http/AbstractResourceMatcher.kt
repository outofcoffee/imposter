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
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMatchOperator
import io.gatehill.imposter.plugin.config.resource.reqbody.BaseRequestBodyConfig
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig
import io.gatehill.imposter.util.BodyQueryUtil
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
    /**
     * {@inheritDoc}
     */
    override fun matchResourceConfig(
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): BasicResourceConfig? {
        val resourceConfigs = filterResourceConfigs(resources, httpExchange)
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
                        LOGGER.warn(
                            "More than one resource config matched an exact path for {} - this is probably a configuration error. Guessing first resource configuration.",
                            LogUtil.describeRequestShort(httpExchange)
                        )
                        return exactMatches[0].resource.config
                    }
                }
            }
        }
    }

    protected open fun filterResourceConfigs(
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): List<MatchedResource> {
        return resources.map { matchRequest(it, httpExchange) }.filter { it.matched }
    }

    /**
     * Determine if the resource configuration matches the current request.
     *
     * @param resource     the resource configuration
     * @param httpExchange the current exchange
     * @return `true` if the resource matches the request, otherwise `false`
     */
    protected abstract fun matchRequest(
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource

    protected fun matchPath(
        httpExchange: HttpExchange,
        resourceConfig: BasicResourceConfig,
        request: HttpRequest,
    ): PathMatchResult {
        // note: path template can be null when a regex route is used
        val pathTemplate = httpExchange.currentRoutePath

        val pathMatch = resourceConfig.path?.let { resourceConfigPath ->
            if (request.path == resourceConfigPath || pathTemplate?.let { it == resourceConfigPath } == true) {
                return@let PathMatchResult.EXACT_MATCH
            } else if (resourceConfigPath.endsWith("*") && request.path.startsWith(resourceConfigPath.substring(0, resourceConfigPath.length - 1))) {
                return@let PathMatchResult.WILDCARD_MATCH
            } else {
                return@let PathMatchResult.NOT_MATCHED
            }
            // if path is un-set, implies match all
        } ?: PathMatchResult.EXACT_MATCH

        return pathMatch
    }

    /**
     * Match the request body against the supplied configuration.
     *
     * @param httpExchange   thc current exchange
     * @param resourceConfig the match configuration
     * @return `true` if the configuration is empty, or the request body matches the configuration, otherwise `false`
     */
    protected fun matchRequestBody(httpExchange: HttpExchange, resourceConfig: BasicResourceConfig): Boolean {
        if (resourceConfig !is RequestBodyResourceConfig) {
            // none configured - implies any match
            return true
        }
        return resourceConfig.requestBody?.allOf?.let { bodyConfigs ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against ${bodyConfigs.size} request body configs for ${LogUtil.describeRequestShort(httpExchange)}: $bodyConfigs")
            }
            bodyConfigs.all { matchUsingBodyConfig(it, httpExchange) }
        } ?: resourceConfig.requestBody?.let { singleRequestBodyConfig ->
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Matching against a single request body config for ${LogUtil.describeRequestShort(httpExchange)}: $singleRequestBodyConfig")
            }
            matchUsingBodyConfig(singleRequestBodyConfig, httpExchange)
        } ?: run {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("No request body config to match for ${LogUtil.describeRequestShort(httpExchange)}")
            }
            // none configured - implies any match
            true
        }
    }

    private fun matchUsingBodyConfig(
        bodyConfig: BaseRequestBodyConfig,
        httpExchange: HttpExchange,
    ): Boolean {
        if (!isNullOrEmpty(bodyConfig.jsonPath)) {
            return matchRequestBodyJsonPath(bodyConfig, httpExchange)
        } else if (!isNullOrEmpty(bodyConfig.xPath)) {
            return matchRequestBodyXPath(bodyConfig, httpExchange)
        } else {
            // none configured - implies any match
            return true
        }
    }

    private fun matchRequestBodyJsonPath(
        bodyConfig: BaseRequestBodyConfig,
        httpExchange: HttpExchange,
    ): Boolean {
        val bodyValue = BodyQueryUtil.queryRequestBodyJsonPath(
            bodyConfig.jsonPath!!,
            httpExchange
        )
        // resource matching always uses strings
        return checkBodyMatch(bodyConfig, bodyValue?.toString())
    }

    private fun matchRequestBodyXPath(
        bodyConfig: BaseRequestBodyConfig,
        httpExchange: HttpExchange,
    ): Boolean {
        val bodyValue = BodyQueryUtil.queryRequestBodyXPath(
            bodyConfig.xPath!!,
            bodyConfig.xmlNamespaces,
            httpExchange
        )
        return checkBodyMatch(bodyConfig, bodyValue)
    }

    private fun checkBodyMatch(bodyConfig: BaseRequestBodyConfig, actualValue: Any?): Boolean {
        // defaults to equality check
        val operator = bodyConfig.operator ?: ResourceMatchOperator.EqualTo

        val matched = when (operator) {
            ResourceMatchOperator.Exists, ResourceMatchOperator.NotExists -> {
                // the expression is checking for the existence of a value using the given query
                return (actualValue != null) == (operator == ResourceMatchOperator.Exists)
            }

            ResourceMatchOperator.EqualTo, ResourceMatchOperator.NotEqualTo ->
                matchUsingEquality(bodyConfig.value, actualValue, operator)

            ResourceMatchOperator.Contains, ResourceMatchOperator.NotContains ->
                matchUsingContains(actualValue, bodyConfig.value, operator)

            ResourceMatchOperator.Matches, ResourceMatchOperator.NotMatches ->
                matchUsingRegex(actualValue, bodyConfig.value, operator)
        }
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace("Body match result for {} {}: {}", bodyConfig.operator, bodyConfig.value, matched)
        }
        return matched
    }

    private fun matchUsingEquality(
        configuredValue: String?,
        actualValue: Any?,
        operator: ResourceMatchOperator,
    ): Boolean {
        val valueMatch = safeEquals(configuredValue, actualValue)

        // apply operator
        return operator === ResourceMatchOperator.EqualTo && valueMatch ||
            operator === ResourceMatchOperator.NotEqualTo && !valueMatch
    }

    private fun matchUsingContains(
        actualValue: Any?,
        configuredValue: String?,
        operator: ResourceMatchOperator,
    ): Boolean {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            actualValue.toString().contains(configuredValue)
        } else {
            false
        }

        // apply operator
        return operator === ResourceMatchOperator.Contains && valueMatch ||
            operator === ResourceMatchOperator.NotContains && !valueMatch
    }

    private fun matchUsingRegex(
        actualValue: Any?,
        configuredValue: String?,
        operator: ResourceMatchOperator,
    ): Boolean {
        val valueMatch = if (actualValue != null && configuredValue != null) {
            val pattern = patternCache.get(configuredValue) { Pattern.compile(configuredValue) }
            pattern.matcher(actualValue.toString()).matches()
        } else {
            false
        }

        // apply operator
        return operator === ResourceMatchOperator.Matches && valueMatch ||
            operator === ResourceMatchOperator.NotMatches && !valueMatch
    }

    protected data class MatchedResource(
        val resource: ResolvedResourceConfig,
        val matched: Boolean,
        val exact: Boolean,
    )

    protected enum class PathMatchResult {
        NOT_MATCHED,
        EXACT_MATCH,
        WILDCARD_MATCH,
    }

    companion object {
        private val LOGGER = LogManager.getLogger(AbstractResourceMatcher::class.java)
        private val patternCache = CacheBuilder.newBuilder().maximumSize(20).build<String, Pattern>()
    }
}
