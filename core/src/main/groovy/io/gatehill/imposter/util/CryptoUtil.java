package io.gatehill.imposter.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class CryptoUtil {
    public static final String DEFAULT_KEYSTORE_PATH = "/keystore/ssl.jks";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "password";

    private CryptoUtil() {
    }

    public static Path getDefaultKeystore(Class<?> clazz) throws URISyntaxException {
        return Paths.get(clazz.getResource(DEFAULT_KEYSTORE_PATH).toURI());
    }
}
