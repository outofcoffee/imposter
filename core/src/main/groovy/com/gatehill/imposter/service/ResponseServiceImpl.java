package com.gatehill.imposter.service;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.config.ResourceConfig;
import com.gatehill.imposter.plugin.config.ResponseConfig;
import com.gatehill.imposter.script.AbstractResponseBehaviour;
import com.gatehill.imposter.script.ResponseBehaviour;
import com.gatehill.imposter.script.ResponseBehaviourImpl;
import com.gatehill.imposter.script.ScriptUtil;
import com.gatehill.imposter.util.HttpUtil;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.gatehill.imposter.script.ResponseBehaviourType.DEFAULT_BEHAVIOUR;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseServiceImpl implements ResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseServiceImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    @Override
    public ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, ResourceConfig config,
                                                  Map<String, Object> additionalContext,
                                                  Map<String, Object> additionalBindings) {

        final ResponseConfig responseConfig = config.getResponseConfig();
        checkNotNull(responseConfig);

        final int statusCode = ofNullable(responseConfig.getStatusCode()).orElse(HttpUtil.HTTP_OK);

        if (null == responseConfig.getScriptFile()) {
            // default behaviour is to use a static response file
            LOGGER.debug("Using default response behaviour for request: {}", routingContext.request().absoluteURI());
            return new ResponseBehaviourImpl()
                    .withStatusCode(statusCode)
                    .withFile(responseConfig.getStaticFile())
                    .usingDefaultBehaviour();
        }

        try {
            LOGGER.debug("Executing script '{}' for request: {}",
                    responseConfig.getScriptFile(), routingContext.request().absoluteURI());

            final Map<String, Object> context = ScriptUtil.buildContext(routingContext, additionalContext);
            LOGGER.trace("Context for request: {}", () -> context);

            final Binding binding = new Binding();
            binding.setVariable("logger", LogManager.getLogger(getScriptName(responseConfig.getScriptFile())));
            binding.setVariable("config", config);
            binding.setVariable("context", context);

            // add custom bindings
            ofNullable(additionalBindings).ifPresent(additionalBinding -> additionalBinding.forEach(binding::setVariable));

            // execute the script and read response behaviour
            final AbstractResponseBehaviour responseBehaviour = executeScript(config, binding);

            // use defaults if not set
            if (DEFAULT_BEHAVIOUR.equals(responseBehaviour.getBehaviourType())) {
                if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                    responseBehaviour.withFile(responseConfig.getStaticFile());
                }
                if (0 == responseBehaviour.getStatusCode()) {
                    responseBehaviour.withStatusCode(statusCode);
                }
            }

            return responseBehaviour;

        } catch (Exception e) {
            throw new RuntimeException(String.format("Error executing script: %s", responseConfig.getScriptFile()), e);
        }
    }

    /**
     * Execute the script and read response behaviour.
     *
     * @param config  the plugin configuration
     * @param binding the script engine bindings
     * @return the response behaviour
     * @throws ExecutionException
     */
    private AbstractResponseBehaviour executeScript(ResourceConfig config, Binding binding) throws ExecutionException {
        final Path scriptFile = Paths.get(imposterConfig.getConfigDir(), config.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        // the script class will be a subclass of AbstractResponseBehaviour
        final CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(AbstractResponseBehaviour.class.getCanonicalName());
        final GroovyShell groovyShell = new GroovyShell(binding, compilerConfig);

        try {
            final AbstractResponseBehaviour script = (AbstractResponseBehaviour) groovyShell.parse(
                    new GroovyCodeSource(scriptFile.toFile(), compilerConfig.getSourceEncoding()));

            script.run();
            return script;

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }

    private String getScriptName(String scriptFile) {
        return scriptFile.replaceAll("\\.groovy", "");
    }

    private InputStream loadResponseAsStream(String responseFile) throws IOException {
        if (null != responseFile) {
            return Files.newInputStream(Paths.get(imposterConfig.getConfigDir(), responseFile));
        } else {
            throw new IllegalStateException("No response file set on ResponseBehaviour");
        }
    }

    @Override
    public JsonArray loadResponseAsJsonArray(ResponseBehaviour behaviour) {
        return loadResponseAsJsonArray(behaviour.getResponseFile());
    }

    @Override
    public JsonArray loadResponseAsJsonArray(String responseFile) {
        if (Strings.isNullOrEmpty(responseFile)) {
            LOGGER.debug("Response file blank - returning empty array");
            return new JsonArray();
        }

        try (InputStream is = loadResponseAsStream(responseFile)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
