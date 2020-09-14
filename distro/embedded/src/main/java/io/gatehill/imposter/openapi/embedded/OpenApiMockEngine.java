package io.gatehill.imposter.openapi.embedded;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.embedded.MockEngine;

import java.net.URI;
import java.util.Arrays;

/**
 * @author pete
 */
public class OpenApiMockEngine extends MockEngine {
    public OpenApiMockEngine(ImposterConfig config) {
        super(config);
    }

    public URI getCombinedSpecificationUri() {
        return URI.create(getBaseUrl("http", getPort()) + OpenApiImposterBuilder.COMBINED_SPECIFICATION_URL);
    }

    public URI getSpecificationUiUri() {
        return URI.create(getBaseUrl("http", getPort()) + OpenApiImposterBuilder.SPECIFICATION_UI_URL);
    }

    @Override
    protected void logStartup() {
        LOGGER.info("Started Imposter mock engine" +
                "\n  Config dir(s): " + Arrays.toString(config.getConfigDirs()) +
                "\n  Base URL: " + getBaseUrl() +
                "\n  Specification UI: " + getSpecificationUiUri());
    }
}
