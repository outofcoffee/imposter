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
package io.gatehill.imposter.scripting.groovy.impl

import groovy.lang.Script
import io.gatehill.imposter.script.*
import io.gatehill.imposter.scripting.groovy.util.ScriptLoader
import java.nio.file.Path

/**
 * @author Pete Cornish
 */
abstract class GroovyResponseBehaviourImpl : Script(), ReadWriteResponseBehaviour {
    private val delegate: ReadWriteResponseBehaviour = ReadWriteResponseBehaviourImpl()

    override val responseHeaders: MutableMap<String, String>
        get() = delegate.responseHeaders

    override val behaviourType: ResponseBehaviourType
        get() = delegate.behaviourType

    override val responseFile: String?
        get() = delegate.responseFile

    override val statusCode: Int
        get() = delegate.statusCode

    override val content: String?
        get() = delegate.content

    override fun template(): MutableResponseBehaviour {
        return delegate.template()
    }

    override val isTemplate: Boolean
        get() = delegate.isTemplate

    override val exampleName: String?
        get() = delegate.exampleName

    override val performanceSimulation: PerformanceSimulationConfig?
        get() = delegate.performanceSimulation

    override val failureType: FailureSimulationType?
        get() = delegate.failureType

    override fun withHeader(header: String, value: String?): MutableResponseBehaviour {
        delegate.withHeader(header, value)
        return this
    }

    override fun withStatusCode(statusCode: Int): MutableResponseBehaviour {
        delegate.withStatusCode(statusCode)
        return this
    }

    override fun withFile(responseFile: String): MutableResponseBehaviour {
        delegate.withFile(responseFile)
        return this
    }

    override fun withContent(content: String?): MutableResponseBehaviour {
        delegate.withContent(content)
        return this
    }

    @Deprecated("Use withContent(String) instead", replaceWith = ReplaceWith("withContent"))
    override fun withData(responseData: String?): MutableResponseBehaviour {
        @Suppress("DEPRECATION")
        delegate.withData(responseData)
        return this
    }

    override fun withExampleName(exampleName: String): MutableResponseBehaviour {
        delegate.withExampleName(exampleName)
        return this
    }

    override fun withEmpty(): MutableResponseBehaviour {
        delegate.withEmpty()
        return this
    }

    override fun usingDefaultBehaviour(): MutableResponseBehaviour {
        delegate.usingDefaultBehaviour()
        return this
    }

    override fun skipDefaultBehaviour(): MutableResponseBehaviour {
        delegate.skipDefaultBehaviour()
        return this
    }

    @Deprecated("Use skipDefaultBehaviour() instead", ReplaceWith("skipDefaultBehaviour()"))
    override fun immediately(): MutableResponseBehaviour {
        @Suppress("DEPRECATION")
        delegate.immediately()
        return this
    }

    override fun respond(): MutableResponseBehaviour {
        delegate.respond()
        return this
    }

    override fun respond(closure: Runnable): MutableResponseBehaviour {
        delegate.respond(closure)
        return this
    }

    override fun and(): MutableResponseBehaviour {
        delegate.and()
        return this
    }

    override fun withPerformance(performance: PerformanceSimulationConfig?): MutableResponseBehaviour {
        delegate.withPerformance(performance)
        return this
    }

    override fun withDelay(exactDelayMs: Int): MutableResponseBehaviour {
        delegate.withDelay(exactDelayMs)
        return this
    }

    override fun withDelayRange(minDelayMs: Int, maxDelayMs: Int): MutableResponseBehaviour {
        delegate.withDelayRange(minDelayMs, maxDelayMs)
        return this
    }

    override fun withFailure(failureType: FailureSimulationType?): MutableResponseBehaviour {
        delegate.withFailure(failureType)
        return this
    }

    fun loadDynamic(relativePath: String): Any {
        val thisScriptPath = super.getProperty(ScriptLoader.contextKeyScriptPath) as Path
        return ScriptLoader.loadDynamic(thisScriptPath, relativePath)
    }
}
