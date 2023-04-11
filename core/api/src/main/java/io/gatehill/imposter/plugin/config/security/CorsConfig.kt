/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.plugin.config.security

import com.fasterxml.jackson.annotation.JsonProperty

class CorsConfig {
    /**
     * The list of origins that are allowed to access the resource.
     * Sets the `Access-Control-Allow-Origin` header to the value of the `Origin` header if
     * it is in the list.
     */
    @field:JsonProperty("allowOrigins")
    val allowOrigins: Any? = null

    /**
     * The list of HTTP methods that are allowed to be used in the `Access-Control-Request-Method` header.
     */
    @field:JsonProperty("allowMethods")
    val allowMethods: List<String>? = null

    /**
     * The list of headers that are allowed to be used in the `Access-Control-Request-Headers` header.
     */
    @field:JsonProperty("allowHeaders")
    val allowHeaders: List<String>? = null

    /**
     * Sets the `Access-Control-Allow-Credentials` header.
     */
    @field:JsonProperty("allowCredentials")
    val allowCredentials = true

    /**
     * The maximum age of the CORS preflight request, in seconds.
     * Sets the `Access-Control-Max-Age` header.
     */
    @field:JsonProperty("maxAge")
    val maxAge = 60L
}
