package io.gatehill.imposter.embedded;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterLaunchException extends RuntimeException {
    public ImposterLaunchException(Exception cause) {
        super(cause);
    }

    public ImposterLaunchException(String message, Exception cause) {
        super(message, cause);
    }
}
