package io.gatehill.imposter.service;

import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.security.ConditionalNameValuePair;
import io.gatehill.imposter.plugin.config.security.MatchOperator;
import io.gatehill.imposter.plugin.config.security.SecurityCondition;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityEffect;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityServiceImpl implements SecurityService {
    private static final Logger LOGGER = LogManager.getLogger(SecurityServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginConfig findConfigPreferringSecurityPolicy(List<? extends PluginConfig> allPluginConfigs) {
        final List<PluginConfig> configsWithSecurity = allPluginConfigs.stream().filter(c -> {
            if (c instanceof SecurityConfigHolder) {
                return nonNull(((SecurityConfigHolder) c).getSecurity());
            }
            return false;
        }).collect(Collectors.toList());

        final PluginConfig selectedConfig;
        if (configsWithSecurity.isEmpty()) {
            selectedConfig = allPluginConfigs.get(0);
        } else if (configsWithSecurity.size() == 1) {
            selectedConfig = configsWithSecurity.get(0);
        } else {
            throw new IllegalStateException("Cannot specify root 'security' configuration block more than once. Ensure only one configuration file contains the root 'security' block.");
        }
        return selectedConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean enforce(SecurityConfig security, RoutingContext routingContext) {
        final PolicyOutcome outcome;
        if (security.getConditions().isEmpty()) {
            outcome = new PolicyOutcome(security.getDefaultEffect(), "default effect");
        } else {
            outcome = evaluatePolicy(security, routingContext);
        }
        return enforceEffect(routingContext, outcome);
    }

    private PolicyOutcome evaluatePolicy(SecurityConfig security, RoutingContext routingContext) {
        final List<SecurityCondition> failed = security.getConditions().stream()
                .filter(c -> !checkCondition(c, routingContext))
                .collect(Collectors.toList());

        if (failed.isEmpty()) {
            return new PolicyOutcome(SecurityEffect.Permit, "all conditions");
        } else {
            return new PolicyOutcome(
                    SecurityEffect.Deny,
                    failed.stream()
                            .map(this::describeCondition)
                            .collect(Collectors.joining(", "))
            );
        }
    }

    /**
     * Determine if the condition permits the request to proceed.
     *
     * @param condition      the security condition
     * @param routingContext the routing context
     * @return {@code true} if the condition permits the request, otherwise {@code false}
     */
    private boolean checkCondition(SecurityCondition condition, RoutingContext routingContext) {
        final List<SecurityEffect> results = new ArrayList<>();

        // query params
        results.addAll(checkCondition(condition.getQueryParams(), routingContext.request().params(), condition.getEffect()));

        // headers
        results.addAll(checkCondition(condition.getRequestHeaders(), routingContext.request().headers(), condition.getEffect()));

        // all must permit
        return results.stream().allMatch(SecurityEffect.Permit::equals);
    }

    /**
     * Determine the effect of each conditional name/value pair and operator.
     * Keys in the request map may be compared in a case-insensitive manner, based
     * on the underlying implementation of the {@link MultiMap}.
     *
     * @param conditionMap    the values from the condition
     * @param requestMap      the values from the request
     * @param conditionEffect the effect of the condition if it is true
     * @return the actual effect based on the values
     */
    private List<SecurityEffect> checkCondition(
            Map<String, ConditionalNameValuePair> conditionMap,
            MultiMap requestMap,
            SecurityEffect conditionEffect
    ) {
        return conditionMap.values().stream().map(conditionValue -> {
            final boolean valueMatch = HttpUtil.safeEquals(
                    requestMap.get(conditionValue.getName()),
                    conditionValue.getValue()
            );

            final boolean matched = ((conditionValue.getOperator() == MatchOperator.EqualTo) && valueMatch) ||
                    ((conditionValue.getOperator() == MatchOperator.NotEqualTo) && !valueMatch);

            final SecurityEffect finalEffect;
            if (matched) {
                finalEffect = conditionEffect;
            } else {
                finalEffect = conditionEffect.invert();
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Condition match for {} {} {}: {}. Request map: {}. Effect: {}",
                        conditionValue.getName(), conditionValue.getOperator(), conditionValue.getValue(), matched, requestMap.entries(), finalEffect);
            }
            return finalEffect;

        }).collect(Collectors.toList());
    }

    private boolean enforceEffect(RoutingContext routingContext, PolicyOutcome outcome) {
        final HttpServerRequest request = routingContext.request();

        if (!SecurityEffect.Permit.equals(outcome.effect)) {
            LOGGER.warn("Denying request {} {} due to security policy - {}",
                    request.method(), request.absoluteURI(), outcome.policySource);

            routingContext.fail(HttpUtil.HTTP_UNAUTHORIZED);
            return false;

        } else {
            LOGGER.trace("Permitting request {} {} due to security policy - {}",
                    request.method(), request.absoluteURI(), outcome.policySource);

            return true;
        }
    }

    private String describeCondition(SecurityCondition condition) {
        final StringBuilder description = new StringBuilder();
        describeConditionPart(description, condition.getQueryParams(), "query conditions");
        describeConditionPart(description, condition.getRequestHeaders(), "header conditions");
        return description.toString();
    }

    private void describeConditionPart(StringBuilder description, Map<String, ConditionalNameValuePair> part, String partType) {
        if (!part.isEmpty()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(partType).append(": [").append(String.join(", ", part.keySet())).append("]");
        }
    }

    private static class PolicyOutcome {
        private final SecurityEffect effect;
        private final String policySource;

        public PolicyOutcome(SecurityEffect effect, String policySource) {
            this.effect = effect;
            this.policySource = policySource;
        }
    }
}
