package io.gatehill.imposter.service;

import com.google.common.base.Preconditions;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptedResponseBehavior;
import io.gatehill.imposter.script.MutableResponseBehaviourImpl;
import io.gatehill.imposter.script.listener.ScriptListener;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class EmbeddedScriptServiceImpl implements ScriptService {
    private ScriptListener listener;

    @Override
    public ScriptedResponseBehavior executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        Preconditions.checkNotNull(listener, "ScriptListener is not set");
        final ScriptedResponseBehavior responseBehaviour = new MutableResponseBehaviourImpl();
        listener.hear(runtimeContext.getExecutionContext(), responseBehaviour);
        return responseBehaviour;
    }

    public void setListener(ScriptListener listener) {
        this.listener = listener;
    }
}
