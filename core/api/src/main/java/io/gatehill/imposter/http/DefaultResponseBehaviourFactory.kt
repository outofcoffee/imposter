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
package io.gatehill.imposter.http

import com.google.common.base.Strings
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl

/**
 * @author Pete Cornish
 */
open class DefaultResponseBehaviourFactory protected constructor() : ResponseBehaviourFactory {
    override fun build(statusCode: Int, resourceConfig: BasicResourceConfig): ReadWriteResponseBehaviour {
        val responseBehaviour = ReadWriteResponseBehaviourImpl()
        populate(statusCode, resourceConfig, responseBehaviour)
        return responseBehaviour
    }

    override fun populate(
        statusCode: Int,
        resourceConfig: BasicResourceConfig,
        responseBehaviour: ReadWriteResponseBehaviour,
    ) {
        if (null == responseBehaviour.behaviourType) {
            resourceConfig.continueToNext?.let { continueToNext ->
                if (continueToNext) responseBehaviour.usingDefaultBehaviour() else responseBehaviour.skipDefaultBehaviour()
            } ?: run {
                if (resourceConfig.isInterceptor) responseBehaviour.skipDefaultBehaviour() else responseBehaviour.usingDefaultBehaviour()
            }
        }

        val responseConfig = resourceConfig.responseConfig
        if (0 == responseBehaviour.statusCode) {
            responseBehaviour.withStatusCode(statusCode)
        }
        if (Strings.isNullOrEmpty(responseBehaviour.responseFile) && !Strings.isNullOrEmpty(responseConfig.file)) {
            responseBehaviour.withFile(responseConfig.file!!)
        }
        if (Strings.isNullOrEmpty(responseBehaviour.content)) {
            responseBehaviour.withContent(responseConfig.content)
        }
        if (responseConfig.isTemplate == true) {
            responseBehaviour.template()
        }
        if (null == responseBehaviour.performanceSimulation) {
            responseBehaviour.withPerformance(responseConfig.performanceDelay)
        }
        if (null == responseBehaviour.failureType) {
            responseBehaviour.withFailureType(responseConfig.failureType)
        }
        responseConfig.headers?.let { headers -> responseBehaviour.responseHeaders.putAll(headers) }
    }

    companion object {
        val instance = DefaultResponseBehaviourFactory()
    }
}