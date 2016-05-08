package com.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseBehaviour {
    int getStatusCode();

    String getResponseFile();

    ResponseBehaviourType getBehaviourType();
}
