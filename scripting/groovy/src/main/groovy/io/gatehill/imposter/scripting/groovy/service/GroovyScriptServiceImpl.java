package io.gatehill.imposter.scripting.groovy.service;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptedResponseBehavior;
import io.gatehill.imposter.scripting.groovy.impl.GroovyResponseBehaviourImpl;
import io.gatehill.imposter.service.ScriptService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GroovyScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(GroovyScriptServiceImpl.class);

    @Override
    public ScriptedResponseBehavior executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        // the script class will be a subclass of AbstractResponseBehaviour
        final CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(GroovyResponseBehaviourImpl.class.getCanonicalName());
        final GroovyShell groovyShell = new GroovyShell(convertBindings(runtimeContext), compilerConfig);

        try {
            final GroovyResponseBehaviourImpl script = (GroovyResponseBehaviourImpl) groovyShell.parse(
                    new GroovyCodeSource(scriptFile.toFile(), compilerConfig.getSourceEncoding()));

            script.run();
            return script;

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }

    private static Binding convertBindings(RuntimeContext runtimeContext) {
        final Binding binding = new Binding();
        runtimeContext.asMap().forEach(binding::setVariable);
        return binding;
    }
}
