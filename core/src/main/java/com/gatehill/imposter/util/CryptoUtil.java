package com.gatehill.imposter.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class CryptoUtil {
    private static final String KEYSTORE_PATH = "/keystore/ssl.jks";
    public static final String KEYSTORE_PASSWORD = "password";

    private CryptoUtil() {
    }

    public static Path getKeystore(Class<?> clazz) throws URISyntaxException {
        return Paths.get(clazz.getResource(KEYSTORE_PATH).toURI());
    }
}
