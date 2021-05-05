package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class DefaultStatusCodeCalculator implements StatusCodeCalculator {
    private static final DefaultStatusCodeCalculator INSTANCE = new DefaultStatusCodeCalculator();

    private DefaultStatusCodeCalculator() {
    }

    public static DefaultStatusCodeCalculator getInstance() {
        return INSTANCE;
    }

    @Override
    public int calculateStatus(ResponseConfigHolder resourceConfig) {
        return ofNullable(resourceConfig.getResponseConfig().getStatusCode()).orElse(200);
    }
}
