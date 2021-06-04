package io.gatehill.imposter.plugin.config.security;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public enum SecurityEffect {
    Permit,
    Deny;

    public SecurityEffect invert() {
        return this == Deny ? Permit : Deny;
    }
}
