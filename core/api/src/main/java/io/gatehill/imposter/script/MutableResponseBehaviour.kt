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
package io.gatehill.imposter.script

/**
 * @author Pete Cornish
 */
interface MutableResponseBehaviour {
    fun withHeader(header: String, value: String?): MutableResponseBehaviour
    fun withStatusCode(statusCode: Int): MutableResponseBehaviour
    fun withFile(responseFile: String): MutableResponseBehaviour
    fun withEmpty(): MutableResponseBehaviour
    fun withContent(content: String?): MutableResponseBehaviour
    @Deprecated("Use withContent(String) instead", replaceWith = ReplaceWith("withContent"))
    fun withData(responseData: String?) = withContent(responseData)
    fun template(): MutableResponseBehaviour
    fun withExampleName(exampleName: String): MutableResponseBehaviour
    fun usingDefaultBehaviour(): MutableResponseBehaviour
    fun skipDefaultBehaviour(): MutableResponseBehaviour
    fun and(): MutableResponseBehaviour
    fun withPerformance(performance: PerformanceSimulationConfig?): MutableResponseBehaviour
    fun withDelay(exactDelayMs: Int): MutableResponseBehaviour
    fun withDelayRange(minDelayMs: Int, maxDelayMs: Int): MutableResponseBehaviour

    /**
     * [failureType] must be a valid [FailureSimulationType]
     */
    fun withFailure(failureType: String): MutableResponseBehaviour
    fun withFailureType(failureType: FailureSimulationType?): MutableResponseBehaviour

    @Deprecated("Use skipDefaultBehaviour() instead", ReplaceWith("skipDefaultBehaviour()"))
    fun immediately(): MutableResponseBehaviour
}
