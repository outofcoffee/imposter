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
