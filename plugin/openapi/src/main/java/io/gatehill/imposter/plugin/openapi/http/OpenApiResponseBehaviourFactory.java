package io.gatehill.imposter.plugin.openapi.http;

import com.google.common.base.Strings;
import io.gatehill.imposter.http.DefaultResponseBehaviourFactory;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiResponseConfig;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;

/**
 * Extends base response behaviour population with specific
 * OpenAPI plugin configuration.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiResponseBehaviourFactory extends DefaultResponseBehaviourFactory {
    @Override
    public void populate(int statusCode, ResponseConfig responseConfig, ReadWriteResponseBehaviour responseBehaviour) {
        super.populate(statusCode, responseConfig, responseBehaviour);

        if (Strings.isNullOrEmpty(responseBehaviour.getExampleName())) {
            responseBehaviour.withExampleName(((OpenApiResponseConfig) responseConfig).getExampleName());
        }
    }
}
