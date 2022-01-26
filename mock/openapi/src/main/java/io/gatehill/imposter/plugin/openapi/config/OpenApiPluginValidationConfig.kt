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
package io.gatehill.imposter.plugin.openapi.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginValidationConfig.ValidationIssueBehaviour.FAIL
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginValidationConfig.ValidationIssueBehaviour.IGNORE
import java.util.*

/**
 * @author Pete Cornish
 */
class OpenApiPluginValidationConfig {
    companion object {
        private val DEFAULT_ISSUE_BEHAVIOUR: ValidationIssueBehaviour

        init {
            DEFAULT_ISSUE_BEHAVIOUR = ValidationIssueBehaviour.from(
                getEnv("IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR"),
                ValidationIssueBehaviour.IGNORE
            )
        }
    }

    /**
     * Could be [Boolean] or [ValidationIssueBehaviour]
     */
    @field:JsonProperty("request")
    private val rawRequest: String? = null

    val request: ValidationIssueBehaviour by lazy {
        ValidationIssueBehaviour.from(rawRequest, DEFAULT_ISSUE_BEHAVIOUR)
    }

    /**
     * Could be [Boolean] or [ValidationIssueBehaviour]
     */
    @field:JsonProperty("response")
    private val rawResponse: String? = null

    val response: ValidationIssueBehaviour by lazy {
        ValidationIssueBehaviour.from(rawResponse, ValidationIssueBehaviour.IGNORE)
    }

    val returnErrorsInResponse = true

    val levels: Map<String, String>? = null

    /**
     * Supports backwards compatible boolean-style values, mapping
     * to [IGNORE] and [FAIL] respectively.
     */
    enum class ValidationIssueBehaviour {
        IGNORE, LOG_ONLY, FAIL;

        companion object {
            fun from(behaviour: String?, defaultBehaviour: ValidationIssueBehaviour): ValidationIssueBehaviour {
                return if ((behaviour?.trim { it <= ' ' } ?: "").isEmpty()) {
                    defaultBehaviour
                } else when (behaviour!!.trim { it <= ' ' }.lowercase(Locale.getDefault())) {
                    "false", "ignore" -> IGNORE
                    "log", "log_only", "log-only" -> LOG_ONLY
                    "true", "fail" -> FAIL
                    else -> throw UnsupportedOperationException("Unknown validation issue behaviour: $behaviour")
                }
            }
        }
    }
}