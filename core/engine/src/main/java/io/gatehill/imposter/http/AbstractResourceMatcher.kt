/*
 * Copyright (c) 2016-2022.
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
import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMatchOperator
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig
import io.gatehill.imposter.util.BodyQueryUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.StringUtil.safeEquals
import org.apache.logging.log4j.LogManager


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

            1 -> LOGGER.debug("Matched resource config for {}", LogUtil.describeRequestShort(httpExchange))
            else -> {
                LOGGER.warn(
                    "More than one resource config found for {} - this is probably a configuration error. Guessing first resource configuration.",
                    LogUtil.describeRequestShort(httpExchange)
                )
            }
        }
        return resourceConfigs[0].config
    }

    protected open fun filterResourceConfigs(
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): List<ResolvedResourceConfig> {
        return resources.filter { res -> isRequestMatch(res, httpExchange) }
    }

    /**
     * Determine if the resource configuration matches the current request.
     *
     * @param resource     the resource configuration
     * @param httpExchange the current exchange
     * @return `true` if the resource matches the request, otherwise `false`
     */
    protected abstract fun isRequestMatch(
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): Boolean

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

        if (!isNullOrEmpty(resourceConfig.requestBody?.jsonPath)) {
            return matchRequestBodyJsonPath(resourceConfig.requestBody!!, httpExchange)
        } else if (!isNullOrEmpty(resourceConfig.requestBody?.xPath)) {
            return matchRequestBodyXPath(resourceConfig.requestBody!!, httpExchange)
        } else {
            // none configured - implies any match
            return true
        }
    }

    private fun matchRequestBodyJsonPath(
        requestBodyConfig: RequestBodyConfig,
        httpExchange: HttpExchange
    ): Boolean {
        val bodyValue = BodyQueryUtil.queryRequestBodyJsonPath(
            requestBodyConfig.jsonPath!!,
            httpExchange
        )
        // resource matching always uses strings
        return checkBodyMatch(requestBodyConfig, bodyValue?.toString())
    }

    private fun matchRequestBodyXPath(
        requestBodyConfig: RequestBodyConfig,
        httpExchange: HttpExchange
    ): Boolean {
        val bodyValue = BodyQueryUtil.queryRequestBodyXPath(
            requestBodyConfig.xPath!!,
            requestBodyConfig.xmlNamespaces,
            httpExchange
        )
        return checkBodyMatch(requestBodyConfig, bodyValue)
    }

    private fun checkBodyMatch(requestBodyConfig: RequestBodyConfig, actualValue: Any?): Boolean {
        requestBodyConfig.exists?.let { shouldExist ->
            // the expression is checking for the existence of a value using the given query,
            // perhaps by using embedded conditional checks
            return (actualValue != null) == shouldExist

        } ?: run {
            val configuredValue = requestBodyConfig.value
            val operator = requestBodyConfig.operator

            // compare the actual value to the configured value
            val matched = when (operator) {
                ResourceMatchOperator.EqualTo, ResourceMatchOperator.NotEqualTo ->
                    matchUsingEquality(configuredValue, actualValue, operator)

                ResourceMatchOperator.Contains, ResourceMatchOperator.NotContains ->
                    matchUsingContains(actualValue, configuredValue, operator)
            }
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Body match result for {} {}: {}", operator, configuredValue, matched)
            }
            return matched
        }
    }

    private fun matchUsingEquality(
        configuredValue: String?,
        actualValue: Any?,
        operator: ResourceMatchOperator
    ): Boolean {
        val valueMatch = safeEquals(configuredValue, actualValue)

        // apply operator
        return operator === ResourceMatchOperator.EqualTo && valueMatch ||
                operator === ResourceMatchOperator.NotEqualTo && !valueMatch
    }

    private fun matchUsingContains(
        actualValue: Any?,
        configuredValue: String?,
        operator: ResourceMatchOperator
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

    companion object {
        private val LOGGER = LogManager.getLogger(AbstractResourceMatcher::class.java)
    }
}
