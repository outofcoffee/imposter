package io.gatehill.imposter.plugin.openapi.service;

import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder;
import io.vertx.ext.web.RoutingContext;

/**
 * Serialises and transmits examples to the client.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseTransmissionService {
    <T> void transmitExample(RoutingContext routingContext, ContentTypedHolder<T> example);
}
