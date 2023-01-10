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
open class ReadWriteResponseBehaviourImpl : ReadWriteResponseBehaviour {
    override var behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR
    override var statusCode = 0
    override var responseFile: String? = null
    override var content: String? = null
    override var isTemplate = false
    override var exampleName: String? = null
    override val responseHeaders: MutableMap<String, String> = mutableMapOf()
    private var behaviourConfigured = false
    override var performanceSimulation: PerformanceSimulationConfig? = null
    override var failureType: FailureSimulationType? = null

    override fun template(): MutableResponseBehaviour {
        isTemplate = true
        return this
    }

    override fun withHeader(header: String, value: String?): MutableResponseBehaviour {
        if (value == null) {
            responseHeaders.remove(header)
        } else {
            responseHeaders[header] = value
        }
        return this
    }

    /**
     * Set the HTTP status code for the response.
     *
     * @param statusCode the HTTP status code
     * @return this
     */
    override fun withStatusCode(statusCode: Int): MutableResponseBehaviour {
        this.statusCode = statusCode
        return this
    }

    /**
     * Respond with the content of a static file.
     *
     * @param responseFile the response file
     * @return this
     */
    override fun withFile(responseFile: String): MutableResponseBehaviour {
        this.responseFile = responseFile
        return this
    }

    /**
     * Respond with empty content, or no records.
     *
     * @return this
     */
    override fun withEmpty(): MutableResponseBehaviour {
        responseFile = null
        return this
    }

    override fun withContent(content: String?): MutableResponseBehaviour {
        this.content = content
        return this
    }

    override fun withExampleName(exampleName: String): MutableResponseBehaviour {
        this.exampleName = exampleName
        return this
    }

    /**
     * Use the plugin's default behaviour to respond
     *
     * @return this
     */
    override fun usingDefaultBehaviour(): MutableResponseBehaviour {
        behaviourConfigured = if (behaviourConfigured) {
            throw IllegalStateException("Response already handled")
        } else {
            true
        }
        behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR
        return this
    }

    /**
     * Skip the plugin's default behaviour when responding.
     *
     * @return this
     */
    override fun skipDefaultBehaviour(): MutableResponseBehaviour {
        behaviourConfigured = if (behaviourConfigured) {
            throw IllegalStateException("Response already handled")
        } else {
            true
        }
        behaviourType = ResponseBehaviourType.SHORT_CIRCUIT
        return this
    }

    /**
     * @return this
     */
    @Deprecated("Use skipDefaultBehaviour() instead", ReplaceWith("skipDefaultBehaviour()"))
    override fun immediately(): MutableResponseBehaviour {
        return skipDefaultBehaviour()
    }

    /**
     * Syntactic sugar.
     *
     * @return this
     */
    override fun respond(): MutableResponseBehaviour {
        return this
    }

    /**
     * Syntactic sugar that executes the Runnable immediately.
     *
     * @return this
     */
    override fun respond(closure: Runnable): MutableResponseBehaviour {
        closure.run()
        return this
    }

    /**
     * Syntactic sugar.
     *
     * @return this
     */
    override fun and(): MutableResponseBehaviour {
        return this
    }

    override fun withPerformance(performance: PerformanceSimulationConfig?): MutableResponseBehaviour {
        performanceSimulation = performance
        return this
    }

    override fun withDelay(exactDelayMs: Int): MutableResponseBehaviour {
        performanceSimulation = PerformanceSimulationConfig().apply {
            this.exactDelayMs = exactDelayMs
        }
        return this
    }

    override fun withDelayRange(minDelayMs: Int, maxDelayMs: Int): MutableResponseBehaviour {
        performanceSimulation = PerformanceSimulationConfig().apply {
            this.minDelayMs = minDelayMs
            this.maxDelayMs = maxDelayMs
        }
        return this
    }

    override fun withFailure(failureType: FailureSimulationType?): MutableResponseBehaviour {
        this.failureType = failureType
        return this
    }
}
