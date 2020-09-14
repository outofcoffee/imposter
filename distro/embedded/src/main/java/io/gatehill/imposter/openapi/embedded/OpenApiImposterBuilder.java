package io.gatehill.imposter.openapi.embedded;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.embedded.ImposterBuilder;
import io.gatehill.imposter.embedded.ImposterLaunchException;
import io.gatehill.imposter.plugin.Plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;

/**
 * @author pete
 */
public class OpenApiImposterBuilder<M extends OpenApiMockEngine, SELF extends OpenApiImposterBuilder<M, SELF>> extends ImposterBuilder<M, SELF> {
    private static final String PLUGIN_FQCN = "io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl";
    static final String COMBINED_SPECIFICATION_URL = "/_spec/combined.json";
    static final String SPECIFICATION_UI_URL = "/_spec/";

    private final List<Path> specificationFiles = new ArrayList<>();

    /**
     * The path to a valid OpenAPI/Swagger specification file.
     *
     * @param specificationFile the directory
     */
    public SELF withSpecificationFile(String specificationFile) {
        return withSpecificationFile(Paths.get(specificationFile));
    }

    /**
     * The path to a valid OpenAPI/Swagger specification file.
     *
     * @param specificationFile the directory
     */
    public SELF withSpecificationFile(Path specificationFile) {
        this.specificationFiles.add(specificationFile);
        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<M> startAsync() {
        final CompletableFuture<M> future;
        try {
            if (!specificationFiles.isEmpty() && !configurationDirs.isEmpty()) {
                throw new IllegalStateException("Must specify only one of specification file or specification directory");
            }
            if (!specificationFiles.isEmpty()) {
                withConfigurationDir(ConfigGenerator.writeImposterConfig(specificationFiles));
            }
            if (isNull(pluginClass)) {
                pluginClass = (Class<? extends Plugin>) Class.forName(PLUGIN_FQCN);
            }
            future = super.startAsync();

        } catch (Exception e) {
            throw new ImposterLaunchException("Error starting Imposter mock engine", e);
        }
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected M buildEngine(ImposterConfig config) {
        return (M) new OpenApiMockEngine(config);
    }
}
