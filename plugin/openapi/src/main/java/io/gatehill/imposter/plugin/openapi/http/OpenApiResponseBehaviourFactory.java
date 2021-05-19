package io.gatehill.imposter.plugin.openapi.http;

import io.gatehill.imposter.http.DefaultResponseBehaviourFactory;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiResponseConfig;
import io.gatehill.imposter.script.MutableResponseBehaviour;

/**
 * Extends base response behaviour population with specific
 * OpenAPI plugin configuration.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiResponseBehaviourFactory extends DefaultResponseBehaviourFactory {
    @Override
    protected void populate(int statusCode, ResponseConfig responseConfig, MutableResponseBehaviour responseBehaviour) {
        super.populate(statusCode, responseConfig, responseBehaviour);
        responseBehaviour.withExampleName(((OpenApiResponseConfig) responseConfig).getExampleName());
    }
}
