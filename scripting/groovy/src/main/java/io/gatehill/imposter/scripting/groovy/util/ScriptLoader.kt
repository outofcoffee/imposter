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
package io.gatehill.imposter.scripting.groovy.util

import groovy.lang.GroovyClassLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path

/**
 * Allows a Groovy script to be loaded dynamically.
 */
object ScriptLoader {
    const val contextKeyScriptPath: String = "io.gatehill.imposter.scripting.groovy.scriptpath"

    private val logger: Logger = LogManager.getLogger(ScriptLoader::class.java)
    private val groovyClassLoader by lazy { GroovyClassLoader() }

    /**
     * Load the script at the given relative path as a class, then return an instance of it.
     */
    fun loadDynamic(thisScriptPath: Path, relativePath: String): Any {
        val otherScriptPath = thisScriptPath.parent.resolve(relativePath)
        logger.debug("Dynamically loading script: {}", otherScriptPath)

        try {
            val clazz = groovyClassLoader.parseClass(otherScriptPath.toFile())
            return clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw RuntimeException("Error dynamically loading script: $otherScriptPath", e)
        }
    }
}
