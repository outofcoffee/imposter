package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.script.MutableResponseBehaviour;
import io.gatehill.imposter.script.MutableResponseBehaviourImpl;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class DefaultResponseBehaviourFactory implements ResponseBehaviourFactory {
    private static final DefaultResponseBehaviourFactory INSTANCE = new DefaultResponseBehaviourFactory();

    protected DefaultResponseBehaviourFactory() {
    }

    public static DefaultResponseBehaviourFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public MutableResponseBehaviour build(int statusCode, ResponseConfig responseConfig) {
        final MutableResponseBehaviourImpl responseBehaviour = new MutableResponseBehaviourImpl();
        populate(statusCode, responseConfig, responseBehaviour);
        return responseBehaviour;
    }

    protected void populate(int statusCode, ResponseConfig responseConfig, MutableResponseBehaviour responseBehaviour) {
        responseBehaviour.withStatusCode(statusCode)
                .withFile(responseConfig.getStaticFile())
                .withData(responseConfig.getStaticData())
                .usingDefaultBehaviour();

        ofNullable(responseConfig.getHeaders()).orElse(emptyMap())
                .forEach(responseBehaviour::withHeader);
    }
}
