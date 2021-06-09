package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface SecurityService {
    boolean enforce(SecurityConfig security, RoutingContext routingContext);
}
