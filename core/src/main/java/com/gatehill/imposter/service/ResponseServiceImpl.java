package com.gatehill.imposter.service;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.model.InvocationContext;
import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ResponseConfig;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.CharStreams;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.gatehill.imposter.model.ResponseBehaviourType.DEFAULT_BEHAVIOUR;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseServiceImpl implements ResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseServiceImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    private Cache<BaseConfig, String> scriptCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    public ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, BaseConfig config, Map<String, Object> bindings) {
        final ResponseConfig responseConfig = config.getResponseConfig();

        final int statusCode = ofNullable(responseConfig.getStatusCode()).orElse(HttpURLConnection.HTTP_OK);

        if (null == responseConfig.getScriptFile()) {
            // default behaviour is to use a static response file
            LOGGER.debug("Using default response behaviour for request: {}", routingContext.request().absoluteURI());
            return new ResponseBehaviour()
                    .withStatusCode(statusCode)
                    .withFile(responseConfig.getStaticFile())
                    .withDefaultBehaviour();
        }

        try {
            LOGGER.debug("Executing script '{}' for request: {}", responseConfig.getScriptFile(), routingContext.request().absoluteURI());

            final InvocationContext invocationContext = InvocationContext.build(routingContext);
            LOGGER.trace("InvocationContext for request: {}", invocationContext);

            final Binding binding = new Binding();
            binding.setVariable("config", config);
            binding.setVariable("context", invocationContext);

            // add custom bindings
            ofNullable(bindings)
                    .map(Map::entrySet)
                    .ifPresent(entries -> entries.forEach(entry -> binding.setVariable(entry.getKey(), entry.getValue())));

            // holds the response
            final AtomicReference<ResponseBehaviour> responseHolder = new AtomicReference<>();
            binding.setVariable("__responseHolder", responseHolder);

            // generate the script
            final String script = generateScript(config);
            LOGGER.trace("Generated script: {}", script);

            // run the script and get the reference to the ResponseBehaviour
            final GroovyShell groovyShell = new GroovyShell(binding);
            groovyShell.evaluate(script);
            final ResponseBehaviour responseBehaviour = responseHolder.get();

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
            throw new RuntimeException(e);
        }
    }

    private String generateScript(BaseConfig config) throws ExecutionException {
        return scriptCache.get(config, () -> {
            final Path scriptFile = Paths.get(imposterConfig.getConfigDir(), config.getResponseConfig().getScriptFile());

            return "def __responseBehaviour = new com.gatehill.imposter.model.ResponseBehaviour() {" +
                    "\n\tpublic void process() throws Exception {\n" +

                    // use default charset (set for JVM)
                    new String(Files.readAllBytes(scriptFile)) +
                    "\n\t}" +
                    "\n}" +
                    "\n__responseHolder.set(__responseBehaviour)" +
                    "\n__responseBehaviour.setContext(context)" +
                    "\n__responseBehaviour.process()";
        });
    }

    @Override
    public InputStream loadResponseAsStream(ImposterConfig imposterConfig, ResponseBehaviour behaviour) throws IOException {
        if (null != behaviour.getResponseFile()) {
            return Files.newInputStream(Paths.get(imposterConfig.getConfigDir(), behaviour.getResponseFile()));
        } else {
            throw new IllegalStateException("No response file set on ResponseBehaviour");
        }
    }

    @Override
    public JsonArray loadResponseAsJsonArray(ImposterConfig imposterConfig, ResponseBehaviour behaviour) {
        try (InputStream is = loadResponseAsStream(imposterConfig, behaviour)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
