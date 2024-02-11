/*
 * Copyright (c) 2016-2024.
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
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader

object ClassLoaderUtil {
    val pluginClassLoader: ClassLoader

    private val pluginDirPath: String?
        get() = EnvVars.getEnv("IMPOSTER_PLUGIN_DIR")?.trim()?.takeIf { it.isNotEmpty() }

    private val archiveExtractDir: File?
        get() = EnvVars.getEnv("IMPOSTER_PLUGIN_EXTRACT_DIR")?.trim()?.takeIf { it.isNotEmpty() }?.let { File(it) }

    private val logger = LogManager.getLogger(ClassLoaderUtil::class.java)
    private const val pluginFileExtension = ".jar"
    private const val archiveFileExtension = ".zip"
    private const val childClassloaderStrategy = "child"

    init {
        pluginClassLoader = determineClassLoader()
    }

    private fun determineClassLoader(): ClassLoader {
        val thisClassloader = ClassLoaderUtil::class.java.classLoader

        return listJars()?.let { jarUrls ->
            if (EnvVars.getEnv("IMPOSTER_PLUGIN_CLASSLOADER_STRATEGY") == childClassloaderStrategy) {
                logger.trace("Plugins will use child-first classloader with plugin directory: {} and files: {}", pluginDirPath, jarUrls)
                return@let PluginClassLoader(jarUrls.toTypedArray(), thisClassloader)
            } else {
                logger.trace("Plugins will use URL classloader with plugin directory: {} and files: {}", pluginDirPath, jarUrls)
                return@let URLClassLoader(jarUrls.toTypedArray(), thisClassloader)
            }

        } ?: run {
            logger.trace("Plugins will use default classloader")
            thisClassloader
        }
    }

    /**
     * Lists all plugin JARs, first extracting any archives.
     */
    private fun listJars(): List<URL>? {
        val pluginDir = pluginDirPath?.let { File(it) }?.also { dir ->
            if (!dir.exists() || !dir.isDirectory) {
                logger.warn("Path $dir is not a valid directory")
                return null
            }
        } ?: run {
            logger.trace("Plugin directory not set")
            return null
        }

        val pluginFiles = pluginDir.listFiles()
            ?.takeIf { it.isNotEmpty() }
            ?: run {
                logger.trace("No plugin files found in {}", pluginDir)
                return null
            }
        logger.trace("Found plugin files in {}: {}", pluginDir, pluginFiles)

        val jars = pluginFiles.filter { it.isPluginJar() }.toMutableList()

        val archives = pluginFiles.filter { it.isFile && it.name.endsWith(archiveFileExtension) }
        val extractBaseDir = archiveExtractDir ?: pluginDir
        jars += archives.flatMap { listJarsFromArchive(extractBaseDir, it) }

        return jars.map { it.toURI().toURL() }
    }

    /**
     * Lists the JARs in the archive's `lib` folder, first extracting the archive to a
     * subdirectory of `extractBaseDir`, if necessary.
     */
    private fun listJarsFromArchive(extractBaseDir: File, archive: File): List<File> {
        try {
            val archiveDir = File(extractBaseDir, archive.nameWithoutExtension)
            if (archiveDir.exists() && archiveDir.isDirectory) {
                logger.trace("Skipping extraction of plugin archive {} as directory {} already exists", archive, archiveDir)
            } else {
                logger.trace("Extracting plugin archive {} to {}", archive, archiveDir)
                archiveDir.mkdirs()
                ArchiveUtil.extractZipFileToDir(archive, archiveDir)
            }
            return File(archiveDir, "lib").listFiles()?.filter { it.isPluginJar() } ?: emptyList()

        } catch (e: Exception) {
            throw IOException("Failed to list JARs in plugin archive $archive", e)
        }
    }

    /**
     * Determines if the file is a plugin JAR.
     */
    private fun File.isPluginJar(): Boolean = isFile && name.endsWith(pluginFileExtension)

    @Suppress("UNCHECKED_CAST")
    fun <T> loadClass(className: String): Class<T> {
        return pluginClassLoader.loadClass(className) as Class<T>
    }

    fun describePluginPath(): String = (pluginDirPath?.let { "$it or " } ?: "") + "program classpath"
}
