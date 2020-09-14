package io.gatehill.imposter.embedded;

/**
 * @author pete
 */
public class ImposterLaunchException extends RuntimeException {
    public ImposterLaunchException(Exception cause) {
        super(cause);
    }

    public ImposterLaunchException(String message, Exception cause) {
        super(message, cause);
    }
}
