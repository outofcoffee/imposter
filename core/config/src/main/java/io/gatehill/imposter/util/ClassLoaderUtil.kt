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

package io.gatehill.imposter.util

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.PluginClassLoader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.URLClassLoader

object ClassLoaderUtil {
    val pluginClassLoader: ClassLoader

    private val pluginDirPath: String?
        get() = EnvVars.getEnv("IMPOSTER_PLUGIN_DIR")?.trim()?.takeIf { it.isNotEmpty() }

    private val logger = LogManager.getLogger(ClassLoaderUtil::class.java)
    private const val pluginFileExtension = ".jar"
    private const val childClassloaderStrategy = "child"

    init {
        pluginClassLoader = determineClassLoader()
    }

    private fun determineClassLoader(): ClassLoader {
        val jarUrls = pluginDirPath?.let { dir ->
            val pluginDir = File(dir).also {
                if (!it.exists() || !it.isDirectory) {
                    logger.warn("Path $dir is not a valid directory")
                    return@let null
                }
            }
            return@let pluginDir.listFiles()
                ?.filter { !it.isDirectory && it.name.endsWith(pluginFileExtension) }
                ?.map { it.toURI().toURL() }
        }

        val thisClassloader = ClassLoaderUtil::class.java.classLoader

        return jarUrls?.let {
            if (EnvVars.getEnv("IMPOSTER_PLUGIN_CLASSLOADER_STRATEGY") == childClassloaderStrategy) {
                logger.trace("Plugins will use child-first classloader with plugin directory: $pluginDirPath and files: $jarUrls")
                return@let PluginClassLoader(jarUrls.toTypedArray(), thisClassloader)
            } else {
                logger.trace("Plugins will use URL classloader with plugin directory: $pluginDirPath and files: $jarUrls")
                return@let URLClassLoader(jarUrls.toTypedArray(), thisClassloader)
            }

        } ?: run {
            logger.trace("Plugins will use default classloader")
            thisClassloader
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> loadClass(className: String): Class<T> {
        return pluginClassLoader.loadClass(className) as Class<T>
    }

    fun describePluginPath(): String = pluginDirPath?.let { "$it or " } + "program classpath"
}
