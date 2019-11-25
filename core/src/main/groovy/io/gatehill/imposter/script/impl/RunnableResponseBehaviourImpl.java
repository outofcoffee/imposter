package io.gatehill.imposter.script.impl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class RunnableResponseBehaviourImpl extends InternalResponseBehaviorImpl {
    /**
     * The main instance method of a script.
     */
    public abstract Object run();
}
