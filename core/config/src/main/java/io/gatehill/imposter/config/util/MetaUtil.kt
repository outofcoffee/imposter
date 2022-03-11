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
package io.gatehill.imposter.config.util

import io.gatehill.imposter.config.model.PluginMetadata
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.Objects
import java.util.Properties
import java.util.jar.Manifest

/**
 * @author Pete Cornish
 */
object MetaUtil {
    private val LOGGER = LogManager.getLogger(MetaUtil::class.java)
    private const val METADATA_MANIFEST = "META-INF/MANIFEST.MF"
    private const val METADATA_DEFAULT_PROPERTIES = "META-INF/imposter.properties"
    private const val METADATA_PLUGIN_PROPERTIES = "META-INF/plugin.properties"
    private const val MANIFEST_VERSION_KEY = "Imposter-Version"
    private var version: String? = null
    private var defaultProperties: Properties? = null

    fun readVersion(): String? {
        if (Objects.isNull(version)) {
            try {
                val manifests = classLoader.getResources(METADATA_MANIFEST)
                while (manifests.hasMoreElements() && Objects.isNull(version)) {
                    manifests.nextElement().openStream().use { manifestStream ->
                        val manifest = Manifest(manifestStream)
                        version = manifest.mainAttributes.getValue(MANIFEST_VERSION_KEY)
                    }
                }
            } catch (ignored: IOException) {
            }
            version = version ?: "unknown"
        }
        return version
    }

    /**
     * Find the plugins that should be loaded, based on metadata on the classpath.
     */
    fun readAllMetaEnabledPlugins(): List<String> {
        // plugin names set in imposter.properties
        val defaultEnabled = readMetaDefaultProperties()
            .getProperty("plugins")?.split(",")
            ?: emptyList()

        // files provided by individual plugins
        val metaPlugins = readPluginMetaFiles()
            .filter { it.value.load == PluginMetadata.PluginLoadStrategy.EAGER }
            .keys

        return defaultEnabled + metaPlugins
    }

    /**
     * Load properties from the default metadata file(s), named [METADATA_DEFAULT_PROPERTIES],
     * found on the classpath.
     */
    fun readMetaDefaultProperties(): Properties {
        return defaultProperties ?: Properties().apply {
            try {
                for (metaFile in classLoader.getResources(METADATA_DEFAULT_PROPERTIES)) {
                    metaFile.openStream().use { properties ->
                        if (!Objects.isNull(properties)) {
                            load(properties)
                        }
                    }
                }
            } catch (e: IOException) {
                LOGGER.warn("Error reading metadata properties from {} - continuing", METADATA_DEFAULT_PROPERTIES, e)
            }
            defaultProperties = this
        }
    }

    /**
     * Load the plugins that should be eagerly loaded, from occurrences of
     * [METADATA_PLUGIN_PROPERTIES] found on the classpath.
     */
    private fun readPluginMetaFiles(): Map<String, PluginMetadata> {
        val metaPlugins = mutableMapOf<String, PluginMetadata>()
        try {
            for (metaFile in classLoader.getResources(METADATA_PLUGIN_PROPERTIES)) {
                metaFile.openStream().use { properties ->
                    properties?.let {
                        val plugin = Properties().apply { load(properties) }
                        val meta = PluginMetadata.parse(plugin)
                        metaPlugins[meta.name] = meta
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.warn("Error reading plugin metadata from {} - continuing", METADATA_DEFAULT_PROPERTIES, e)
        }

        LOGGER.trace("Read {} plugins from metadata: {}", metaPlugins.size, metaPlugins)
        return metaPlugins
    }

    private val classLoader: ClassLoader
        get() = MetaUtil::class.java.classLoader
}
