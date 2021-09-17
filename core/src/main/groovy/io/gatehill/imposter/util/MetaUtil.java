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

package io.gatehill.imposter.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.Manifest;

import static java.util.Objects.isNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MetaUtil {
    private static final Logger LOGGER = LogManager.getLogger(MetaUtil.class);
    private static final String METADATA_MANIFEST = "META-INF/MANIFEST.MF";
    private static final String METADATA_PROPERTIES = "META-INF/imposter.properties";
    private static final String MANIFEST_VERSION_KEY = "Imposter-Version";

    private static String version;
    private static Properties metaProperties;

    private MetaUtil() {
    }

    public static String readVersion() {
        if (isNull(version)) {
            try {
                final Enumeration<URL> manifests = getClassLoader().getResources(METADATA_MANIFEST);
                while (manifests.hasMoreElements() && isNull(version)) {
                    try (final InputStream manifestStream = manifests.nextElement().openStream()) {
                        final Manifest manifest = new Manifest(manifestStream);
                        version = manifest.getMainAttributes().getValue(MANIFEST_VERSION_KEY);
                    }
                }
            } catch (IOException ignored) {
            }

            version = Optional.ofNullable(version).orElse("unknown");
        }
        return version;
    }

    public static Properties readMetaProperties() {
        if (isNull(metaProperties)) {
            metaProperties = new Properties();
            try (InputStream properties = getClassLoader().getResourceAsStream(METADATA_PROPERTIES)) {
                if (!isNull(properties)) {
                    metaProperties.load(properties);
                }
            } catch (IOException e) {
                LOGGER.warn("Error reading metadata properties from {} - continuing", METADATA_PROPERTIES, e);
            }
        }
        return metaProperties;
    }

    private static ClassLoader getClassLoader() {
        return MetaUtil.class.getClassLoader();
    }
}
