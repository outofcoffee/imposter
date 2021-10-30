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

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.*
import java.util.jar.Manifest

/**
 * @author Pete Cornish
 */
object MetaUtil {
    private val LOGGER = LogManager.getLogger(MetaUtil::class.java)
    private const val METADATA_MANIFEST = "META-INF/MANIFEST.MF"
    private const val METADATA_PROPERTIES = "META-INF/imposter.properties"
    private const val MANIFEST_VERSION_KEY = "Imposter-Version"
    private var version: String? = null
    private var metaProperties: Properties? = null

    @JvmStatic
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
            version = Optional.ofNullable(version).orElse("unknown")
        }
        return version
    }

    fun readMetaProperties(): Properties {
        if (Objects.isNull(metaProperties)) {
            metaProperties = Properties()
            try {
                classLoader.getResourceAsStream(METADATA_PROPERTIES).use { properties ->
                    if (!Objects.isNull(properties)) {
                        metaProperties!!.load(properties)
                    }
                }
            } catch (e: IOException) {
                LOGGER.warn("Error reading metadata properties from {} - continuing", METADATA_PROPERTIES, e)
            }
        }
        return metaProperties!!
    }

    private val classLoader: ClassLoader
        get() = MetaUtil::class.java.classLoader
}