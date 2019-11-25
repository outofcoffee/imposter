package io.gatehill.imposter.script.impl;

import io.gatehill.imposter.script.InternalResponseBehavior;
import io.gatehill.imposter.script.MutableResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.util.HttpUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InternalResponseBehaviorImpl implements InternalResponseBehavior {
    private ResponseBehaviourType behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
    private int statusCode = HttpUtil.HTTP_OK;
    private String responseFile;
    private String responseData;
    private Map<String, String> responseHeaders = new HashMap<>();
    private boolean behaviourConfigured;

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
    public ResponseBehaviourType getBehaviourType() {
        return behaviourType;
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

    @Override
    public MutableResponseBehaviour withData(String responseData) {
        this.responseData = responseData;
        return this;
    }
}
