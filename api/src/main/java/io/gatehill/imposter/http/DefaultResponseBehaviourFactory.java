package io.gatehill.imposter.http;

import com.google.common.base.Strings;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl;

import static java.util.Objects.isNull;
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
    public ReadWriteResponseBehaviour build(int statusCode, ResponseConfig responseConfig) {
        final ReadWriteResponseBehaviourImpl responseBehaviour = new ReadWriteResponseBehaviourImpl();
        populate(statusCode, responseConfig, responseBehaviour);
        return responseBehaviour;
    }

    @Override
    public void populate(int statusCode, ResponseConfig responseConfig, ReadWriteResponseBehaviour responseBehaviour) {
        if (0 == responseBehaviour.getStatusCode()) {
            responseBehaviour.withStatusCode(statusCode);
        }
        if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
            responseBehaviour.withFile(responseConfig.getStaticFile());
        }
        if (Strings.isNullOrEmpty(responseBehaviour.getResponseData())) {
            responseBehaviour.withData(responseConfig.getStaticData());
        }
        if (responseConfig.isTemplate()) {
            responseBehaviour.template();
        }
        if (isNull(responseBehaviour.getPerformanceSimulation())) {
            responseBehaviour
                    .withPerformance(responseConfig.getPerformanceDelay());
        }

        ofNullable(responseConfig.getHeaders()).ifPresent(headers ->
                responseBehaviour.getResponseHeaders().putAll(headers)
        );
    }
}
