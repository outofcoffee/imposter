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
package io.gatehill.imposter.plugin.config.capture

import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.plugin.config.flex.TypeParser

/**
 * A capture configuration that supports path parameters, query parameters,
 * request headers, request body, expressions and constants.
 *
 * @author Pete Cornish
 */
open class CaptureConfig(
    @field:JsonProperty("pathParam")
    val pathParam: String? = null,

    @field:JsonProperty("queryParam")
    val queryParam: String? = null,

    @field:JsonProperty("formParam")
    val formParam: String? = null,

    @field:JsonProperty("requestHeader")
    val requestHeader: String? = null,

    /**
     * Use [requestBody] instead.
     */
    @field:JsonProperty("requestBody")
    private val _requestBody: BodyCaptureConfig? = null,

    @field:JsonProperty("expression")
    val expression: String? = null,

    @field:JsonProperty("const")
    val constValue: String? = null,
) {
    companion object : TypeParser<String?, CaptureConfig> {
        override fun parse(raw: String?) = CaptureConfig(expression = raw)
    }

    @Deprecated("Use requestBody.jsonPath instead")
    @field:JsonProperty("jsonPath")
    private val legacyJsonPath: String? = null

    val requestBody: BodyCaptureConfig by lazy {
        @Suppress("DEPRECATION")
        BodyCaptureConfig.parse(_requestBody, legacyJsonPath)
    }

    override fun toString(): String {
        @Suppress("DEPRECATION")
        return "CaptureConfig(pathParam=$pathParam, queryParam=$queryParam, formParam=$formParam, requestHeader=$requestHeader, legacyJsonPath=$legacyJsonPath, expression=$expression, constValue=$constValue, requestBody=$requestBody)"
    }
}
