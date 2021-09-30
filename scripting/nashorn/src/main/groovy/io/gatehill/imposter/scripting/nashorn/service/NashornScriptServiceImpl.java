/*
 * Copyright (c) 2016-2021.
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

package io.gatehill.imposter.scripting.nashorn.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.scripting.common.JavaScriptUtil;
import io.gatehill.imposter.scripting.nashorn.shim.ConsoleShim;
import io.gatehill.imposter.service.ScriptService;
import io.gatehill.imposter.util.MetricsUtil;
import io.micrometer.core.instrument.Gauge;
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
    private static final String METRIC_SCRIPT_CACHE_ENTRIES = "script.cache.entries";

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

        MetricsUtil.doIfMetricsEnabled(METRIC_SCRIPT_CACHE_ENTRIES, registry ->
                Gauge.builder(METRIC_SCRIPT_CACHE_ENTRIES, compiledScripts::size)
                        .description("The number of cached compiled scripts")
                        .register(registry)
        );
    }

    @Override
    public ReadWriteResponseBehaviour executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());

        LOGGER.trace("Executing script file: {}", scriptFile);
        try {
            final CompiledScript compiledScript = getCompiledScript(scriptFile);
            final SimpleBindings bindings = new SimpleBindings(runtimeContext.asMap());

            // JS environment affordances
            bindings.put("console", new ConsoleShim(bindings));

            return (ReadWriteResponseBehaviour) compiledScript.eval(bindings);

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
