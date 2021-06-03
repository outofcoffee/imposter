package io.gatehill.imposter.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.security.HttpHeader;
import io.gatehill.imposter.plugin.config.security.SecurityCondition;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityEffect;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityServiceImpl implements SecurityService {
    private static final Logger LOGGER = LogManager.getLogger(SecurityServiceImpl.class);

    @Override
    public void enforce(ImposterConfig imposterConfig, Vertx vertx, Router router, PluginConfig config) {
        if (!(config instanceof SecurityConfigHolder)) {
            LOGGER.debug("No security configuration found");
            return;
        }

        final SecurityConfig security = ((SecurityConfigHolder) config).getSecurity();
        if (isNull(security)) {
            LOGGER.debug("No security configuration found");
            return;
        }

        LOGGER.debug("Enforcing security configuration [{} conditions]", security.getConditions().size());

        router.route().handler(AsyncUtil.handleRoute(imposterConfig, vertx, rc -> {
            final PolicyOutcome outcome;
            if (security.getConditions().isEmpty()) {
                outcome = new PolicyOutcome(security.getDefaultEffect(), "default effect");
            } else {
                outcome = evaluatePolicy(security, rc);
            }
            enforceEffect(rc, outcome);
        }));
    }

    private PolicyOutcome evaluatePolicy(SecurityConfig security, RoutingContext rc) {
        final MultiMap requestHeaders = rc.request().headers();

        final List<SecurityCondition> failed = security.getConditions().stream()
                .filter(c -> !checkCondition(requestHeaders, c))
                .collect(Collectors.toList());

        final SecurityEffect permitted;
        final String policySource;
        if (failed.isEmpty()) {
            permitted = SecurityEffect.Permit;
            policySource = "all conditions";
        } else {
            permitted = SecurityEffect.Deny;
            policySource = describeCondition(failed.get(0));
        }

        return new PolicyOutcome(permitted, policySource);
    }

    private boolean checkCondition(MultiMap requestHeaders, SecurityCondition condition) {
        return condition.getParsedHeaders().stream().allMatch(h -> {
            final boolean headerMatch = HttpUtil.safeEquals(requestHeaders.get(h.getName()), h.getValue());
            switch (h.getOperator()) {
                case EqualTo:
                    return headerMatch;
                case NotEqualTo:
                    return !headerMatch;
                default:
                    throw new IllegalStateException("Unsupported header match operator: " + h.getOperator());
            }
        });
    }

    private void enforceEffect(RoutingContext routingContext, PolicyOutcome outcome) {
        final HttpServerRequest request = routingContext.request();

        if (!SecurityEffect.Permit.equals(outcome.getEffect())) {
            LOGGER.warn("Denying request {} {} due to security policy - {}",
                    request.method(), request.path(), outcome.getPolicySource());

            routingContext.fail(HttpUtil.HTTP_UNAUTHORIZED);

        } else {
            LOGGER.trace("Permitting request {} {} due to security policy - {}",
                    request.method(), request.path(), outcome.getPolicySource());

            routingContext.next();
        }
    }

    private String describeCondition(SecurityCondition condition) {
        return "header condition mismatch: [" +
                condition.getParsedHeaders().stream()
                        .map(HttpHeader::getName)
                        .collect(Collectors.joining(", ")) +
                "]";
    }

    private static class PolicyOutcome {
        private final SecurityEffect effect;
        private final String policySource;

        public PolicyOutcome(SecurityEffect effect, String policySource) {
            this.effect = effect;
            this.policySource = policySource;
        }

        public SecurityEffect getEffect() {
            return effect;
        }

        public String getPolicySource() {
            return policySource;
        }
    }
}
