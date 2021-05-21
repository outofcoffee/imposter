package io.gatehill.imposter.scripting.nashorn.service

import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.scripting.common.JavaScriptUtil
import io.gatehill.imposter.service.ScriptService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.inject.Inject
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class NashhornScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(NashhornScriptServiceImpl.class);

    @Inject
    private ScriptEngineManager scriptEngineManager;

    @Override
    ReadWriteResponseBehaviour executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");

        try {
            return (ReadWriteResponseBehaviour) scriptEngine.eval(JavaScriptUtil.wrapScript(scriptFile), new SimpleBindings(runtimeContext.asMap()));

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }
}
