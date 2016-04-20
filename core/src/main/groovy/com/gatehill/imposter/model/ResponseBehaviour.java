package com.gatehill.imposter.model;

import com.google.common.collect.Maps;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseBehaviour {
    private Map<String, Object> context;
    private ResponseBehaviourType behaviourType;
    private int statusCode;
    private String responseFile;
    private boolean behaviourConfigured;

    /**
     * Accessible in Groovy style {@code context.uri} etc.
     *
     * @return the context
     */
    public Map<String, Object> getContext() {
        return context;
    }

    public void setInvocationContext(InvocationContext invocationContext) {
        // build the context
        context = Maps.newHashMap();
        context.put("uri", invocationContext.getUri());
        context.put("params", invocationContext.getParams());
        ofNullable(invocationContext.getAdditional()).ifPresent(context::putAll);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseFile() {
        return responseFile;
    }

    public void process() throws Exception {
        // no op
    }

    public ResponseBehaviour withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ResponseBehaviour withFile(String responseFile) {
        this.responseFile = responseFile;
        return this;
    }

    public ResponseBehaviour withEmpty() {
        this.responseFile = null;
        return this;
    }

    public ResponseBehaviour withDefaultBehaviour() {
        if (behaviourConfigured) {
            throw new IllegalStateException("Response already handled");
        } else {
            behaviourConfigured = true;
        }

        behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
        return this;
    }

    public ResponseBehaviour immediately() {
        if (behaviourConfigured) {
            throw new IllegalStateException("Response already handled");
        } else {
            behaviourConfigured = true;
        }

        behaviourType = ResponseBehaviourType.IMMEDIATE_RESPONSE;
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return
     */
    public ResponseBehaviour respond() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return
     */
    public ResponseBehaviour and() {
        return this;
    }

    public ResponseBehaviourType getBehaviourType() {
        return behaviourType;
    }
}
