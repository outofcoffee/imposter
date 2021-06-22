package io.gatehill.imposter.scripting.nashorn.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.scripting.common.JavaScriptUtil;
import io.gatehill.imposter.service.ScriptService;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class NashornScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(NashornScriptServiceImpl.class);
    private static final String ENV_SCRIPT_CACHE_ENTRIES = "IMPOSTER_SCRIPT_CACHE_ENTRIES";
    private static final int DEFAULT_SCRIPT_CACHE_ENTRIES = 20;

    private final NashornScriptEngine scriptEngine;

    /**
     * Holds compiled scripts, with maximum number of entries determined by the environment
     * variable {@link #ENV_SCRIPT_CACHE_ENTRIES}.
     */
    private final Cache<Path, CompiledScript> compiledScripts = CacheBuilder.newBuilder()
            .maximumSize(ofNullable(System.getenv(ENV_SCRIPT_CACHE_ENTRIES)).map(Integer::parseInt).orElse(DEFAULT_SCRIPT_CACHE_ENTRIES))
            .build();

    @Inject
    public NashornScriptServiceImpl(ScriptEngineManager scriptEngineManager) {
        scriptEngine = (NashornScriptEngine) scriptEngineManager.getEngineByName("nashorn");
    }

    @Override
    public ReadWriteResponseBehaviour executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());

        LOGGER.trace("Executing script file: {}", scriptFile);
        try {
            final CompiledScript compiledScript = getCompiledScript(scriptFile);
            return (ReadWriteResponseBehaviour) compiledScript.eval(new SimpleBindings(runtimeContext.asMap()));

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }

    private CompiledScript getCompiledScript(Path scriptFile) throws ExecutionException {
        return compiledScripts.get(scriptFile, () -> {
            try {
                LOGGER.trace("Compiling script file: {}", scriptFile);
                final long compileStartMs = System.currentTimeMillis();

                final CompiledScript cs = scriptEngine.compile(JavaScriptUtil.wrapScript(scriptFile));
                LOGGER.debug("Script: {} compiled in {}ms", scriptFile, (System.currentTimeMillis() - compileStartMs));
                return cs;

            } catch (Exception e) {
                throw new RuntimeException("Failed to compile script: " + scriptFile, e);
            }
        });
    }
}
