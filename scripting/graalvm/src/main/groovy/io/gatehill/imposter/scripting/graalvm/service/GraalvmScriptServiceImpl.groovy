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

package io.gatehill.imposter.scripting.graalvm.service

import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
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

        // quieten interpreter mode warning until native graal compiler included in module path - see:
        // https://www.graalvm.org/reference-manual/js/RunOnJDK/
        System.setProperty('polyglot.engine.WarnInterpreterOnly', 'false')
    }

    @Override
    ReadWriteResponseBehaviour executeScript(PluginConfig pluginConfig, ResponseConfigHolder resourceConfig, RuntimeContext runtimeContext) {
        final Path scriptFile = Paths.get(pluginConfig.getParentDir().getAbsolutePath(), resourceConfig.getResponseConfig().getScriptFile());
        LOGGER.trace("Executing script file: {}", scriptFile);

        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("graal.js");
        final Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowAllAccess", true);

        try {
            return (ReadWriteResponseBehaviour) scriptEngine.eval(JavaScriptUtil.wrapScript(scriptFile), new SimpleBindings(runtimeContext.asMap()));

        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }
}
