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

import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig
import io.gatehill.imposter.service.script.InlineScriptService
import io.gatehill.imposter.util.CollectionUtil.convertKeysToLowerCase
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.StringUtil.safeEquals
import java.util.*


/**
 * Matches resources using elements of the HTTP request.
 *
 * @author Pete Cornish
 */
class SingletonResourceMatcher : AbstractResourceMatcher() {
    private val inlineScriptService: InlineScriptService by lazy { InjectorUtil.getInstance() }

    override fun filterResourceConfigs(
        pluginConfig: PluginConfig,
        resources: List<ResolvedResourceConfig>,
        httpExchange: HttpExchange,
    ): List<MatchedResource> {
        var matched = super.filterResourceConfigs(pluginConfig, resources, httpExchange)

        // find the most specific, by filtering those that match by those that specify parameters
        matched = filterByPairs(matched, ResolvedResourceConfig::pathParams)
        matched = filterByPairs(matched, ResolvedResourceConfig::queryParams)
        matched = filterByPairs(matched, ResolvedResourceConfig::formParams)
        matched = filterByPairs(matched, ResolvedResourceConfig::requestHeaders)
        matched = filterByEvalMatcher(matched)

        return matched
    }

    /**
     * {@inheritDoc}
     */
    override fun matchRequest(
        pluginConfig: PluginConfig,
        resource: ResolvedResourceConfig,
        httpExchange: HttpExchange,
    ): MatchedResource {
        val resourceConfig = resource.config
        val request = httpExchange.request

        val pathMatch = matchPath(httpExchange, resourceConfig, request)
        val methodMatch = matchMethod(resourceConfig, request)
        val pathParamsMatch = matchPairs(request.pathParams, resource.pathParams, true)
        val queryParamsMatch = matchPairs(request.queryParams, resource.queryParams, true)
        val formParamsMatch = matchPairs(request.formParams, resource.formParams, true)
        val headersMatch = matchPairs(request.headers, resource.requestHeaders, false)
        val bodyMatch = matchRequestBody(httpExchange, pluginConfig, resource.config)
        val evalMatch = inlineScriptService.evalScript(httpExchange, pluginConfig, resource.config)

        val matchResults = listOf(pathMatch, methodMatch, pathParamsMatch, queryParamsMatch, formParamsMatch, headersMatch, bodyMatch, evalMatch)
        return determineMatch(matchResults, resource, httpExchange)
    }

    private fun matchMethod(
        resourceConfig: BasicResourceConfig,
        request: HttpRequest,
    ) = if (resourceConfig is MethodResourceConfig && null != resourceConfig.method) {
        if (request.method == resourceConfig.method) {
            ResourceMatchResult.EXACT_MATCH
        } else {
            ResourceMatchResult.NOT_MATCHED
        }
    } else {
        // unspecified
        ResourceMatchResult.NO_CONFIG
    }

    private fun filterByPairs(
        resourceConfigs: List<MatchedResource>,
        pairsSupplier: (ResolvedResourceConfig) -> Map<String, String>,
    ): List<MatchedResource> {
        val configsWithPairs = resourceConfigs.filter { res -> pairsSupplier(res.resource).isNotEmpty() }

        return configsWithPairs.ifEmpty {
            // no resource configs specified params - don't filter
            resourceConfigs
        }
    }

    private fun filterByEvalMatcher(resourceConfigs: List<MatchedResource>) = resourceConfigs.filter {
        inlineScriptService.hasInlineScript(it.resource.config)
    }.ifEmpty {
        // no resource configs specified eval - don't filter
        resourceConfigs
    }

    /**
     * If the resource contains parameter configuration, check they are all present.
     * If the configuration contains no parameters, then this evaluates to true.
     * Additional parameters not in the configuration are ignored.
     *
     * @param resourceMap           the configured parameters to match
     * @param requestMap            the parameters from the request (e.g. query or path)
     * @param caseSensitiveKeyMatch whether to match keys case-sensitively
     * @return `true` if the configured parameters match the request, otherwise `false`
     */
    private fun matchPairs(
        requestMap: Map<String, String>,
        resourceMap: Map<String, String>,
        caseSensitiveKeyMatch: Boolean,
    ): ResourceMatchResult {
        // none configured
        if (resourceMap.isEmpty()) {
            return ResourceMatchResult.NO_CONFIG
        }

        // optionally normalise request map
        val comparisonRequestMap = if (caseSensitiveKeyMatch) requestMap else convertKeysToLowerCase(requestMap)

        // all members of the config map must be present in the request for it to match
        val allEqual = resourceMap.all { (key, value) ->
            val configKey: String = if (caseSensitiveKeyMatch) key else key.lowercase(Locale.getDefault())
            safeEquals(comparisonRequestMap[configKey], value)
        }
        return if (allEqual) ResourceMatchResult.EXACT_MATCH else ResourceMatchResult.NOT_MATCHED
    }

    companion object {
        val instance = SingletonResourceMatcher()
    }
}
