package com.gatehill.imposter.model;

import com.gatehill.imposter.util.HttpUtil;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseBehaviour {
    private ResponseBehaviourType behaviourType = ResponseBehaviourType.DEFAULT_BEHAVIOUR;
    private int statusCode = HttpUtil.HTTP_OK;
    private String responseFile;
    private boolean behaviourConfigured;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseFile() {
        return responseFile;
    }

    public ResponseBehaviourType getBehaviourType() {
        return behaviourType;
    }

    /**
     * This method is overridden by the script that instantiates a subclass of this class.
     * The actual user-provided scripts are placed in the body of this method for execution.
     *
     * @throws Exception
     */
    public void process() throws Exception {
        // no op
    }

    /**
     * Set the HTTP status code for the response.
     *
     * @param statusCode the HTTP status code
     * @return this
     */
    public ResponseBehaviour withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Respond with the content of a static file.
     *
     * @param responseFile the response file
     * @return this
     */
    public ResponseBehaviour withFile(String responseFile) {
        this.responseFile = responseFile;
        return this;
    }

    /**
     * Respond with empty content, or no records.
     *
     * @return this
     */
    public ResponseBehaviour withEmpty() {
        this.responseFile = null;
        return this;
    }

    /**
     * Use the plugin's default behaviour to respond
     *
     * @return this
     */
    public ResponseBehaviour usingDefaultBehaviour() {
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
     * @return this
     */
    public ResponseBehaviour respond() {
        return this;
    }

    /**
     * Syntactic sugar that executes the Runnable immediately.
     *
     * @return this
     */
    public ResponseBehaviour respond(Runnable closure) {
        closure.run();
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return this
     */
    public ResponseBehaviour and() {
        return this;
    }
}
