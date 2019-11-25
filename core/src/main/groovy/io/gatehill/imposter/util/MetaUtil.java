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
