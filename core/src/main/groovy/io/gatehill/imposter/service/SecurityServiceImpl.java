/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.service;

import io.gatehill.imposter.lifecycle.EngineLifecycleHooks;
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.security.ConditionalNameValuePair;
import io.gatehill.imposter.plugin.config.security.MatchOperator;
import io.gatehill.imposter.plugin.config.security.SecurityCondition;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityEffect;
import io.gatehill.imposter.service.security.SecurityLifecycleListenerImpl;
import io.gatehill.imposter.util.CollectionUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.StringUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish
 */
public class SecurityServiceImpl implements SecurityService {
    private static final Logger LOGGER = LogManager.getLogger(SecurityServiceImpl.class);

    @Inject
    public SecurityServiceImpl(SecurityLifecycleHooks securityLifecycle, SecurityLifecycleListenerImpl securityListener) {
        securityLifecycle.registerListener(securityListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginConfig findConfigPreferringSecurityPolicy(List<? extends PluginConfig> allPluginConfigs) {
        // sanity check
        if (allPluginConfigs.isEmpty()) {
            throw new IllegalStateException("No plugin configurations");
        }

        final List<PluginConfig> configsWithSecurity = allPluginConfigs.stream().filter(c -> {
            if (c instanceof SecurityConfigHolder) {
                return nonNull(((SecurityConfigHolder) c).getSecurityConfig());
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
        results.addAll(checkCondition(condition.getQueryParams(), routingContext.request().params(), condition.getEffect(), true));

        // headers
        results.addAll(checkCondition(condition.getRequestHeaders(), routingContext.request().headers(), condition.getEffect(), false));

        // all must permit
        return results.stream().allMatch(SecurityEffect.Permit::equals);
    }

    /**
     * Determine the effect of each conditional name/value pair and operator.
     * Keys in the request map may be compared in a case-insensitive manner, based
     * on the value of caseSensitiveKeyMatch.
     *
     * @param conditionMap          the values from the condition
     * @param requestMap            the values from the request
     * @param conditionEffect       the effect of the condition if it is true
     * @param caseSensitiveKeyMatch whether to match the keys case-sensitively
     * @return the actual effect based on the values
     */
    private List<SecurityEffect> checkCondition(
            Map<String, ConditionalNameValuePair> conditionMap,
            MultiMap requestMap,
            SecurityEffect conditionEffect,
            boolean caseSensitiveKeyMatch
    ) {
        final Map<String, String> comparisonMap = caseSensitiveKeyMatch ?
                CollectionUtil.asMap(requestMap) :
                CollectionUtil.convertKeysToLowerCase(requestMap);

        return conditionMap.values().stream().map(conditionValue -> {
            final boolean valueMatch = StringUtil.safeEquals(
                    comparisonMap.get(caseSensitiveKeyMatch ? conditionValue.getName() : conditionValue.getName().toLowerCase()),
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
                        conditionValue.getName(), conditionValue.getOperator(), conditionValue.getValue(), matched, comparisonMap.entrySet(), finalEffect);
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
