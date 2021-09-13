package io.gatehill.imposter.embedded;

import io.gatehill.imposter.ImposterConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Wraps a mock engine instance, providing access to its URL.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class MockEngine {
    protected static final Logger LOGGER = LogManager.getLogger(MockEngine.class);
    protected final ImposterConfig config;

    public MockEngine(ImposterConfig config) {
        this.config = config;
    }

    public URL getBaseUrl(String scheme, int port) {
        try {
            return new URL(scheme + "://" + ImposterBuilder.HOST + ":" + port);
        } catch (MalformedURLException e) {
            throw new ImposterLaunchException(e);
        }
    }

    public URL getBaseUrl(String scheme) {
        return getBaseUrl(scheme, getPort());
    }

    public URL getBaseUrl() {
        return getBaseUrl("http");
    }

    public int getPort() {
        return config.getListenPort();
    }

    protected void logStartup() {
        LOGGER.info("Started Imposter mock engine" +
                "\n  Config dir(s): " + Arrays.toString(config.getConfigDirs()) +
                "\n  Base URL: " + getBaseUrl());
    }
}
