package io.gatehill.imposter.script.impl;

import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class RunnableResponseBehaviourImpl extends ReadWriteResponseBehaviourImpl {
    /**
     * The main instance method of a script.
     */
    public abstract Object run();
}
