package com.gatehill.imposter.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseBehaviour {
    private InvocationContext context;
    private ResponseBehaviourType behaviourType;
    private int statusCode;
    private String responseFile;
    private boolean behaviourConfigured;

    public InvocationContext getContext() {
        return context;
    }

    public void setContext(InvocationContext context) {
        this.context = context;
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
