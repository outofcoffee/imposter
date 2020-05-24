package io.gatehill.imposter.script.listener;

import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ScriptedResponseBehavior;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptListener {
    void hear(ExecutionContext context, ScriptedResponseBehavior responseBehaviour);
}
