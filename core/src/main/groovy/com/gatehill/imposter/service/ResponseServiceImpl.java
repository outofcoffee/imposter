package com.gatehill.imposter.service;

import com.gatehill.imposter.plugin.config.ResourceConfig;
import com.gatehill.imposter.plugin.config.ResponseConfig;
import com.gatehill.imposter.script.ResponseBehaviour;
import com.gatehill.imposter.script.ScriptUtil;
import com.gatehill.imposter.script.MutableResponseBehaviour;
import com.gatehill.imposter.script.impl.MutableResponseBehaviourImpl;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.annotation.GroovyImpl;
import com.gatehill.imposter.util.annotation.JavascriptImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import static com.gatehill.imposter.script.ResponseBehaviourType.DEFAULT_BEHAVIOUR;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseServiceImpl implements ResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseServiceImpl.class);

    @Inject
    @GroovyImpl
    private ScriptService groovyScriptService;

    @Inject
    @JavascriptImpl
    private ScriptService javascriptScriptService;

    @Override
    public ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, ResourceConfig config,
                                                  Map<String, Object> additionalContext,
                                                  Map<String, Object> additionalBindings) {

        final ResponseConfig responseConfig = config.getResponseConfig();
        checkNotNull(responseConfig);

        final int statusCode = ofNullable(responseConfig.getStatusCode()).orElse(HttpUtil.HTTP_OK);

        if (Objects.isNull(responseConfig.getScriptFile())) {
            // default behaviour is to use a static response file
            LOGGER.debug("Using default response behaviour for request: {}", routingContext.request().absoluteURI());
            return new MutableResponseBehaviourImpl()
                    .withStatusCode(statusCode)
                    .withFile(responseConfig.getStaticFile())
                    .usingDefaultBehaviour();
        }

        try {
            LOGGER.debug("Executing script '{}' for request: {}",
                    responseConfig.getScriptFile(), routingContext.request().absoluteURI());

            final Map<String, Object> context = ScriptUtil.buildContext(routingContext, additionalContext);
            LOGGER.trace("Context for request: {}", () -> context);

            final Map<String, Object> bindings = Maps.newHashMap();
            bindings.put("logger", LogManager.getLogger(determineScriptName(responseConfig.getScriptFile())));
            bindings.put("config", config);
            bindings.put("context", context);

            // add custom bindings
            ofNullable(additionalBindings).ifPresent(bindings::putAll);

            // execute the script and read response behaviour
            final MutableResponseBehaviour responseBehaviour = fetchScriptService(config.getResponseConfig().getScriptFile()).executeScript(config, bindings);

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

    private ScriptService fetchScriptService(String scriptFile) {
        final String scriptExtension;
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            scriptExtension = scriptFile.substring(dotIndex + 1);
        } else {
            scriptExtension = "";
        }

        switch (scriptExtension.toLowerCase()) {
            case "groovy":
                return groovyScriptService;
            case "js":
                return javascriptScriptService;
            default:
                throw new RuntimeException("Unable to determine script engine from script file name: " + scriptFile);
        }
    }

    private String determineScriptName(String scriptFile) {
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            return scriptFile.substring(0, dotIndex);
        } else {
            return scriptFile;
        }
    }

    private InputStream loadResponseAsStream(ResourceConfig config, String responseFile) throws IOException {
        if (null != responseFile) {
            return Files.newInputStream(Paths.get(config.getParentDir().getAbsolutePath(), responseFile));
        } else {
            throw new IllegalStateException("No response file set on ResponseBehaviour");
        }
    }

    @Override
    public JsonArray loadResponseAsJsonArray(ResourceConfig config, ResponseBehaviour behaviour) {
        return loadResponseAsJsonArray(config, behaviour.getResponseFile());
    }

    @Override
    public JsonArray loadResponseAsJsonArray(ResourceConfig config, String responseFile) {
        if (Strings.isNullOrEmpty(responseFile)) {
            LOGGER.debug("Response file blank - returning empty array");
            return new JsonArray();
        }

        try (InputStream is = loadResponseAsStream(config, responseFile)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
