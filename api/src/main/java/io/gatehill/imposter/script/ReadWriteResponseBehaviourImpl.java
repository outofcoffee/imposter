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

package io.gatehill.imposter.script;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ReadWriteResponseBehaviourImpl implements ReadWriteResponseBehaviour {
    private ResponseBehaviourType behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
    private int statusCode;
    private String responseFile;
    private String responseData;
    private boolean template;
    private String exampleName;
    private final Map<String, String> responseHeaders = new HashMap<>();
    private boolean behaviourConfigured;
    private PerformanceSimulationConfig performanceSimulationConfig;

    @Override
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getResponseFile() {
        return responseFile;
    }

    @Override
    public String getResponseData() {
        return responseData;
    }

    @Override
    public MutableResponseBehaviour template() {
        this.template = true;
        return this;
    }

    @Override
    public boolean isTemplate() {
        return template;
    }

    @Override
    public String getExampleName() {
        return exampleName;
    }

    @Override
    public ResponseBehaviourType getBehaviourType() {
        return behaviourType;
    }

    @Override
    public PerformanceSimulationConfig getPerformanceSimulation() {
        return performanceSimulationConfig;
    }

    @Override
    public MutableResponseBehaviour withHeader(String header, String value) {
        if (value == null) {
            responseHeaders.remove(header);
        } else {
            responseHeaders.put(header, value);
        }
        return this;
    }

    /**
     * Set the HTTP status code for the response.
     *
     * @param statusCode the HTTP status code
     * @return this
     */
    @Override
    public MutableResponseBehaviour withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Respond with the content of a static file.
     *
     * @param responseFile the response file
     * @return this
     */
    @Override
    public MutableResponseBehaviour withFile(String responseFile) {
        this.responseFile = responseFile;
        return this;
    }

    /**
     * Respond with empty content, or no records.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour withEmpty() {
        this.responseFile = null;
        return this;
    }

    @Override
    public MutableResponseBehaviour withData(String responseData) {
        this.responseData = responseData;
        return this;
    }

    @Override
    public MutableResponseBehaviour withExampleName(String exampleName) {
        this.exampleName = exampleName;
        return this;
    }

    /**
     * Use the plugin's default behaviour to respond
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour usingDefaultBehaviour() {
        if (behaviourConfigured) {
            throw new IllegalStateException("Response already handled");
        } else {
            behaviourConfigured = true;
        }

        behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
        return this;
    }

    /**
     * Skip the plugin's default behaviour when responding.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour skipDefaultBehaviour() {
        if (behaviourConfigured) {
            throw new IllegalStateException("Response already handled");
        } else {
            behaviourConfigured = true;
        }

        behaviourType = ResponseBehaviourType.SHORT_CIRCUIT;
        return this;
    }

    /**
     * @deprecated use {@link #skipDefaultBehaviour()} instead
     * @return this
     */
    @Deprecated
    @Override
    public MutableResponseBehaviour immediately() {
        return skipDefaultBehaviour();
    }

    /**
     * Syntactic sugar.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour respond() {
        return this;
    }

    /**
     * Syntactic sugar that executes the Runnable immediately.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour respond(Runnable closure) {
        closure.run();
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour and() {
        return this;
    }

    @Override
    public MutableResponseBehaviour withPerformance(PerformanceSimulationConfig performance) {
        performanceSimulationConfig = performance;
        return this;
    }

    @Override
    public MutableResponseBehaviour withDelay(int exactDelayMs) {
        performanceSimulationConfig = new PerformanceSimulationConfig();
        performanceSimulationConfig.setExactDelayMs(exactDelayMs);
        return this;
    }

    @Override
    public MutableResponseBehaviour withDelayRange(int minDelayMs, int maxDelayMs) {
        performanceSimulationConfig = new PerformanceSimulationConfig();
        performanceSimulationConfig.setMinDelayMs(minDelayMs);
        performanceSimulationConfig.setMaxDelayMs(maxDelayMs);
        return this;
    }
}
