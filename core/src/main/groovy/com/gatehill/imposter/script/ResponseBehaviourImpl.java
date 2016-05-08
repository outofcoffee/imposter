package com.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseBehaviourImpl extends AbstractResponseBehaviour {
    @Override
    public Object run() {
        throw new UnsupportedOperationException("This method should not be invoked outside of the script engine.");
    }
}
