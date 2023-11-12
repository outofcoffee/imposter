/*
 * Copyright (c) 2016-2023.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.openapi.embedded;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.embedded.ImposterBuilder;
import io.gatehill.imposter.embedded.ImposterLaunchException;
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl;
import io.gatehill.imposter.util.ClassLoaderUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Extends the base builder with options specific to the OpenAPI plugin.
 * <p>
 * Example using an OpenAPI spec as the source:
 * <pre>
 * MockEngine imposter = new OpenApiImposterBuilder<>()
 *             .withSpecificationFile(specFile)
 *             .startBlocking();
 *
 * // mockEndpoint will look like <a href="http://localhost:5234/v1/pets">...</a>
 * String mockEndpoint = imposter.getBaseUrl() + "/v1/pets";
 *
 * // Your component under test can interact with this endpoint to get
 * // simulated HTTP responses, in place of a real endpoint.
 * </pre>
 *
 * @author Pete Cornish
 */
public class OpenApiImposterBuilder<M extends OpenApiMockEngine, SELF extends OpenApiImposterBuilder<M, SELF>> extends ImposterBuilder<M, SELF> {
    private static final String PLUGIN_FQCN = OpenApiPluginImpl.class.getCanonicalName();
    static final String COMBINED_SPECIFICATION_URL = OpenApiPluginImpl.getCombinedSpecPath();
    static final String SPECIFICATION_UI_URL = OpenApiPluginImpl.getSpecPathPrefix() + "/";

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
            if (pluginClasses.isEmpty()) {
                withPluginClass(ClassLoaderUtil.INSTANCE.loadClass(PLUGIN_FQCN));
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
