package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface SecurityService {
    /**
     * Find a plugin configuration with a 'security' block if one is non-null.
     * <p>
     * Only zero or one configurations can specify the 'security' block.
     * If none are found, the first configuration is returned, indicating no security policy is specified.
     * If more than one configuration has a security block, an {@link IllegalStateException} is thrown.
     *
     * @param allPluginConfigs all plugin configurations
     * @return a single plugin configuration that <i>may</i> have a security configuration.
     */
    PluginConfig findConfigPreferringSecurityPolicy(List<? extends PluginConfig> allPluginConfigs);

    /**
     * Enforces the given security policy on the current request.
     * <p>
     * If the request is to be denied, then this method sends HTTP 401 to the {@link RoutingContext}.
     * If the request is to be permitted, no modification is made to the {@link RoutingContext}.
     *
     * @param security       the security policy
     * @param routingContext the current request
     * @return {@code true} of the request is permitted to continue, otherwise {@code false}
     */
    boolean enforce(SecurityConfig security, RoutingContext routingContext);
}
