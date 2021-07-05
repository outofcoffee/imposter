package io.gatehill.imposter.service.security;

import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleListener;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;
import io.gatehill.imposter.service.SecurityService;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityLifecycleListener implements ImposterLifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger(SecurityLifecycleListener.class);

    @Inject
    private SecurityService securityService;

    @Override
    public boolean isRequestPermitted(
            ResponseConfigHolder rootResourceConfig,
            ResponseConfigHolder resourceConfig,
            List<ResolvedResourceConfig> resolvedResourceConfigs,
            RoutingContext routingContext
    ) {
        final HttpServerRequest request = routingContext.request();

        final SecurityConfig security = getSecurityConfig(rootResourceConfig, resourceConfig);
        if (nonNull(security)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Enforcing security policy [{} conditions] for: {} {}",
                        security.getConditions().size(),
                        request.method(),
                        request.absoluteURI()
                );
            }
            return securityService.enforce(security, routingContext);

        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No security policy found for: {} {}", request.method(), request.absoluteURI());
            }
            return true;
        }
    }

    private SecurityConfig getSecurityConfig(ResponseConfigHolder rootResourceConfig, ResponseConfigHolder resourceConfig) {
        SecurityConfig security = getSecurityConfig(resourceConfig);
        if (isNull(security)) {
            // IMPORTANT: if no resource security, fall back to root security
            security = getSecurityConfig(rootResourceConfig);
        }
        return security;
    }

    private SecurityConfig getSecurityConfig(ResponseConfigHolder resourceConfig) {
        if (!(resourceConfig instanceof SecurityConfigHolder)) {
            return null;
        }
        return ((SecurityConfigHolder) resourceConfig).getSecurity();
    }
}
