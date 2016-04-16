package com.gatehill.imposter.model;

import com.gatehill.imposter.ImposterConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseBehaviour {
    private int statusCode;
    private boolean handled;
    private Path responseFile;

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isHandled() {
        return handled;
    }

    public Path getResponseFile() {
        return responseFile;
    }

    public static ResponseBehaviour buildStatic(int statusCode, ImposterConfig imposterConfig, String staticFile) {
        final ResponseBehaviour behaviour = new ResponseBehaviour();
        behaviour.statusCode = statusCode;
        behaviour.handled = false;
        behaviour.responseFile = Paths.get(imposterConfig.getConfigDir(), staticFile);
        return behaviour;
    }

    public static ResponseBehaviour buildHandled(int statusCode) {
        final ResponseBehaviour behaviour = new ResponseBehaviour();
        behaviour.statusCode = statusCode;
        behaviour.handled = true;
        return behaviour;
    }
}
