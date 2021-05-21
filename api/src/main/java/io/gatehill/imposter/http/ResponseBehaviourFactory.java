package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@FunctionalInterface
public interface ResponseBehaviourFactory {
    ReadWriteResponseBehaviour build(int statusCode, ResponseConfig responseConfig);
}
