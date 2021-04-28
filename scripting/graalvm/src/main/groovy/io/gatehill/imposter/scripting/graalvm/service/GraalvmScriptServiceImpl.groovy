package io.gatehill.imposter.scripting.graalvm.service

import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptedResponseBehavior
import io.gatehill.imposter.scripting.common.JavaScriptUtil
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.inject.Inject
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class GraalvmScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(GraalvmScriptServiceImpl.class);

    @Inject
    private ScriptEngineManager scriptEngineManager;

    GraalvmScriptServiceImpl() {
        // see https://www.graalvm.org/reference-manual/js/NashornMigrationGuide/#nashorn-compatibility-mode
        System.setProperty('polyglot.js.nashorn-compat', 'true');
    }

    @Override
    ScriptedResponseBehavior executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("graal.js");
        final Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowAllAccess", true);

        try {
            return (ScriptedResponseBehavior) scriptEngine.eval(JavaScriptUtil.wrapScript(scriptFile), new SimpleBindings(runtimeContext.asMap()));

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }
}
