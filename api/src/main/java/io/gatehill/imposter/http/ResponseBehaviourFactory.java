package io.gatehill.imposter.http;

import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseBehaviourFactory {
    ReadWriteResponseBehaviour build(int statusCode, ResponseConfig responseConfig);

    /**
     * Sets (but does not overwrite) values on the {@link io.gatehill.imposter.script.ResponseBehaviour}, from
     * those on {@link ResponseConfig}.
     *
     * @param statusCode the status code
     * @param responseConfig
     * @param responseBehaviour
     */
    void populate(int statusCode, ResponseConfig responseConfig, ReadWriteResponseBehaviour responseBehaviour);
}
