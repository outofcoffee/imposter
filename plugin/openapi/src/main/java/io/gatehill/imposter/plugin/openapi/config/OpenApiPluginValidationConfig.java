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

package io.gatehill.imposter.plugin.openapi.config;

import io.gatehill.imposter.util.EnvVars;

import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish
 */
public class OpenApiPluginValidationConfig {
    private static final ValidationIssueBehaviour DEFAULT_ISSUE_BEHAVIOUR;

    static {
        DEFAULT_ISSUE_BEHAVIOUR = ValidationIssueBehaviour.from(
                EnvVars.getEnv("IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR"),
                ValidationIssueBehaviour.IGNORE
        );
    }

    /**
     * Could be {@link Boolean} or {@link ValidationIssueBehaviour}
     */
    private String request;

    /**
     * Could be {@link Boolean} or {@link ValidationIssueBehaviour}
     */
    private String response;

    private Boolean returnErrorsInResponse = true;
    private Map<String, String> levels;

    /**
     * Cached default request validation issue behaviour.
     */
    private ValidationIssueBehaviour requestBehaviour;

    /**
     * Cached default response validation issue behaviour.
     */
    private ValidationIssueBehaviour responseBehaviour;

    public ValidationIssueBehaviour getRequest() {
        if (isNull(requestBehaviour)) {
            requestBehaviour = ValidationIssueBehaviour.from(request, DEFAULT_ISSUE_BEHAVIOUR);
        }
        return requestBehaviour;
    }

    public ValidationIssueBehaviour getResponse() {
        if (isNull(responseBehaviour)) {
            responseBehaviour = ValidationIssueBehaviour.from(response, ValidationIssueBehaviour.IGNORE);
        }
        return responseBehaviour;
    }

    public Boolean getReturnErrorsInResponse() {
        return returnErrorsInResponse;
    }

    public Map<String, String> getLevels() {
        return levels;
    }

    /**
     * Supports backwards compatible boolean-style values, mapping
     * to {@link #IGNORE} and {@link #FAIL} respectively.
     */
    public enum ValidationIssueBehaviour {
        IGNORE,
        LOG_ONLY,
        FAIL;

        static ValidationIssueBehaviour from(String behaviour, ValidationIssueBehaviour defaultBehaviour) {
            if (ofNullable(behaviour).map(String::trim).orElse("").isEmpty()) {
                return defaultBehaviour;
            }
            switch (behaviour.trim().toLowerCase()) {
                case "false":
                case "ignore":
                    return IGNORE;

                case "log":
                case "log_only":
                case "log-only":
                    return LOG_ONLY;

                case "true":
                case "fail":
                    return FAIL;

                default:
                    throw new UnsupportedOperationException("Unknown validation issue behaviour: " + behaviour);
            }
        }
    }
}
