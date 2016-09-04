package com.gatehill.imposter.script.impl;

import com.gatehill.imposter.script.InternalResponseBehavior;
import com.gatehill.imposter.script.ResponseBehaviourType;
import com.gatehill.imposter.script.MutableResponseBehaviour;
import com.gatehill.imposter.util.HttpUtil;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InternalResponseBehaviorImpl implements InternalResponseBehavior {
    private ResponseBehaviourType behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
    private int statusCode = HttpUtil.HTTP_OK;
    private String responseFile;
    private boolean behaviourConfigured;

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getResponseFile() {
        return responseFile;
    }

    @Override
    public ResponseBehaviourType getBehaviourType() {
        return behaviourType;
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
     * Skip the plugin's default behaviour and respond immediately.
     *
     * @return this
     */
    @Override
    public MutableResponseBehaviour immediately() {
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
}
