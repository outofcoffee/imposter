package io.gatehill.imposter.script;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResponseBehaviour {
    Map<String, String> getResponseHeaders();

    int getStatusCode();

    String getResponseFile();

    String getResponseData();

    ResponseBehaviourType getBehaviourType();
}
