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
package io.gatehill.imposter.plugin.config.resource

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.plugin.config.resource.conditional.ConditionalNameValuePair
import io.gatehill.imposter.plugin.config.resource.conditional.MatchOperator
import io.gatehill.imposter.plugin.config.resource.expression.ExpressionMatcherConfig
import io.gatehill.imposter.plugin.config.resource.expression.ExpressionMatchersConfigHolder
import io.gatehill.imposter.plugin.config.resource.request.FormParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.LegacyQueryParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.MethodResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.PathParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.QueryParamsResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.RequestBodyConfig
import io.gatehill.imposter.plugin.config.resource.request.RequestBodyResourceConfig
import io.gatehill.imposter.plugin.config.resource.request.RequestHeadersResourceConfig
import io.gatehill.imposter.plugin.config.steps.StepConfig
import io.gatehill.imposter.plugin.config.steps.StepsConfigHolder

/**
 * @author Pete Cornish
 */
open class RestResourceConfig(
    /**
     * Raw configuration. Use [pathParams] instead.
     */
    @field:JsonProperty("pathParams")
    protected var rawPathParams: Map<String, Any>? = null,

    /**
     * Raw configuration. Use [queryParams] instead.
     */
    @field:JsonProperty("queryParams")
    @field:JsonAlias("params")
    protected var rawQueryParams: Map<String, Any>? = null,

    /**
    * Raw configuration. Use [requestHeaders] instead.
    */
    @field:JsonProperty("requestHeaders")
    protected var rawRequestHeaders: Map<String, Any>? = null,

    /**
    * Raw configuration. Use [formParams] instead.
    */
    @field:JsonProperty("formParams")
    private val rawFormParams: Map<String, Any>? = null,

) : AbstractResourceConfig(), MethodResourceConfig, PathParamsResourceConfig,
    QueryParamsResourceConfig, LegacyQueryParamsResourceConfig, RequestHeadersResourceConfig, FormParamsResourceConfig,
    RequestBodyResourceConfig, EvalResourceConfig, PassthroughResourceConfig, StepsConfigHolder,
    ExpressionMatchersConfigHolder {

    @field:JsonProperty("method")
    override var method: HttpMethod? = null

    override val pathParams: Map<String, ConditionalNameValuePair> by lazy {
        rawPathParams?.let { ConditionalNameValuePair.parse(it) } ?: emptyMap()
    }

    override val queryParams: Map<String, ConditionalNameValuePair> by lazy {
        rawQueryParams?.let { ConditionalNameValuePair.parse(it) } ?: emptyMap()
    }

    override val requestHeaders: Map<String, ConditionalNameValuePair> by lazy {
        rawRequestHeaders?.let { ConditionalNameValuePair.parse(it) } ?: emptyMap()
    }

    override val formParams: Map<String, ConditionalNameValuePair> by lazy {
        rawFormParams?.let { ConditionalNameValuePair.parse(it) } ?: emptyMap()
    }

    @field:JsonProperty("requestBody")
    override var requestBody: RequestBodyConfig? = null

    @field:JsonProperty("eval")
    override var eval: String? = null

    @field:JsonProperty("passthrough")
    override var passthrough: String? = null

    /**
     * Backward compatibility for deprecated `params` property.
     * Only [MatchOperator.EqualTo] matches are supported.
     */
    @get:JsonIgnore
    override val params: Map<String, String>?
        get() = queryParams
                .filter { it.value.operator == MatchOperator.EqualTo && it.value.value != null }
                .mapValues { it.value.value!! }

    @field:JsonProperty("steps")
    override val steps: List<StepConfig>? = null

    @field:JsonProperty("allOf")
    override var allOf: List<ExpressionMatcherConfig>? = null

    override fun toString(): String {
        return "RestResourceConfig(parent=${super.toString()}, method=$method, pathParams=$pathParams, queryParams=$queryParams, formParams=$formParams, requestHeaders=$requestHeaders, requestBody=$requestBody, eval=$eval, passthrough=$passthrough, allOf=$allOf)"
    }
}
