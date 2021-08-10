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
