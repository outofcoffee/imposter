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

package io.gatehill.imposter.scripting.groovy.impl;

import groovy.lang.Script;
import io.gatehill.imposter.script.MutableResponseBehaviour;
import io.gatehill.imposter.script.PerformanceSimulationConfig;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class GroovyResponseBehaviourImpl extends Script implements ReadWriteResponseBehaviour {
    private final ReadWriteResponseBehaviour delegate = new ReadWriteResponseBehaviourImpl();

    @Override
    public Map<String, String> getResponseHeaders() {
        return delegate.getResponseHeaders();
    }

    @Override
    public ResponseBehaviourType getBehaviourType() {
        return delegate.getBehaviourType();
    }

    @Override
    public String getResponseFile() {
        return delegate.getResponseFile();
    }

    @Override
    public int getStatusCode() {
        return delegate.getStatusCode();
    }

    @Override
    public String getResponseData() {
        return delegate.getResponseData();
    }

    @Override
    public MutableResponseBehaviour template() {
        return delegate.template();
    }

    @Override
    public boolean isTemplate() {
        return delegate.isTemplate();
    }

    @Override
    public String getExampleName() {
        return delegate.getExampleName();
    }

    @Override
    public PerformanceSimulationConfig getPerformanceSimulation() {
        return delegate.getPerformanceSimulation();
    }

    @Override
    public MutableResponseBehaviour withHeader(String header, String value) {
        delegate.withHeader(header, value);
        return this;
    }

    @Override
    public MutableResponseBehaviour withStatusCode(int statusCode) {
        delegate.withStatusCode(statusCode);
        return this;
    }

    @Override
    public MutableResponseBehaviour withFile(String responseFile) {
        delegate.withFile(responseFile);
        return this;
    }

    @Override
    public MutableResponseBehaviour withData(String responseData) {
        delegate.withData(responseData);
        return this;
    }

    @Override
    public MutableResponseBehaviour withExampleName(String exampleName) {
        delegate.withExampleName(exampleName);
        return this;
    }

    @Override
    public MutableResponseBehaviour withEmpty() {
        delegate.withEmpty();
        return this;
    }

    @Override
    public MutableResponseBehaviour usingDefaultBehaviour() {
        delegate.usingDefaultBehaviour();
        return this;
    }

    @Override
    public MutableResponseBehaviour skipDefaultBehaviour() {
        delegate.skipDefaultBehaviour();
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

    @Override
    public MutableResponseBehaviour respond() {
        delegate.respond();
        return this;
    }

    @Override
    public MutableResponseBehaviour respond(Runnable closure) {
        delegate.respond(closure);
        return this;
    }

    @Override
    public MutableResponseBehaviour and() {
        delegate.and();
        return this;
    }

    @Override
    public MutableResponseBehaviour withPerformance(PerformanceSimulationConfig performance) {
        delegate.withPerformance(performance);
        return this;
    }

    @Override
    public MutableResponseBehaviour withDelay(int exactDelayMs) {
        delegate.withDelay(exactDelayMs);
        return this;
    }

    @Override
    public MutableResponseBehaviour withDelayRange(int minDelayMs, int maxDelayMs) {
        delegate.withDelayRange(minDelayMs, maxDelayMs);
        return this;
    }
}
