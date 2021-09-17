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

package io.gatehill.imposter.http;

import com.google.common.base.Strings;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class DefaultResponseBehaviourFactory implements ResponseBehaviourFactory {
    private static final DefaultResponseBehaviourFactory INSTANCE = new DefaultResponseBehaviourFactory();

    protected DefaultResponseBehaviourFactory() {
    }

    public static DefaultResponseBehaviourFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public ReadWriteResponseBehaviour build(int statusCode, ResponseConfig responseConfig) {
        final ReadWriteResponseBehaviourImpl responseBehaviour = new ReadWriteResponseBehaviourImpl();
        populate(statusCode, responseConfig, responseBehaviour);
        return responseBehaviour;
    }

    @Override
    public void populate(int statusCode, ResponseConfig responseConfig, ReadWriteResponseBehaviour responseBehaviour) {
        if (0 == responseBehaviour.getStatusCode()) {
            responseBehaviour.withStatusCode(statusCode);
        }
        if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
            responseBehaviour.withFile(responseConfig.getStaticFile());
        }
        if (Strings.isNullOrEmpty(responseBehaviour.getResponseData())) {
            responseBehaviour.withData(responseConfig.getStaticData());
        }
        if (responseConfig.isTemplate()) {
            responseBehaviour.template();
        }
        if (isNull(responseBehaviour.getPerformanceSimulation())) {
            responseBehaviour
                    .withPerformance(responseConfig.getPerformanceDelay());
        }

        ofNullable(responseConfig.getHeaders()).ifPresent(headers ->
                responseBehaviour.getResponseHeaders().putAll(headers)
        );
    }
}
