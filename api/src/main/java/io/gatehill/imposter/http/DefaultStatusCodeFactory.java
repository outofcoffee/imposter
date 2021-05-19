package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class DefaultStatusCodeFactory implements StatusCodeFactory {
    private static final DefaultStatusCodeFactory INSTANCE = new DefaultStatusCodeFactory();

    private DefaultStatusCodeFactory() {
    }

    public static DefaultStatusCodeFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public int calculateStatus(ResponseConfigHolder resourceConfig) {
        return ofNullable(resourceConfig.getResponseConfig().getStatusCode()).orElse(200);
    }
}
