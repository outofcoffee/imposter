/*
 * Copyright (c) 2024.
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

package io.gatehill.imposter.config.loader

import com.fasterxml.jackson.databind.JsonMappingException
import io.gatehill.imposter.config.ConfigReference
import io.gatehill.imposter.config.LoadedConfig
import io.gatehill.imposter.config.LoadedConfigImpl
import io.gatehill.imposter.config.expression.EnvEvaluator
import io.gatehill.imposter.config.expression.SystemEvaluator
import io.gatehill.imposter.config.model.LightweightConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import io.gatehill.imposter.expression.util.ExpressionUtil
import io.gatehill.imposter.plugin.config.BasicPluginConfig
import java.io.File
import java.io.IOException

/**
 * YAML (and JSON) implementation of a configuration loader.
 *
 * @author Pete Cornish
 */
class YamlConfigLoaderImpl : ConfigLoader<LoadedConfig> {
    private var expressionEvaluators: Map<String, ExpressionEvaluator<*>> = emptyMap()

    init {
        initInterpolators(EnvVars.getEnv())
    }

    fun initInterpolators(environment: Map<String, String>) {
        // reset the environment used by the expression evaluator
        expressionEvaluators = mapOf(
            "env" to EnvEvaluator(environment),
            "system" to SystemEvaluator,
        )
    }

    /**
     * Reads the contents of the configuration file, performing necessary string substitutions.
     *
     * @param configRef             the configuration reference
     * @return the loaded configuration
     */
    override fun readPluginConfig(configRef: ConfigReference): LoadedConfig {
        val configFile = configRef.file
        try {
            val rawContents = configFile.readText()
            val parsedContents = ExpressionUtil.eval(rawContents, expressionEvaluators, nullifyUnsupported = false)

            val config = ConfigUtil.lookupMapper(configFile).readValue(parsedContents, LightweightConfig::class.java)
            config.plugin
                ?: throw IllegalStateException("No plugin specified in configuration file: $configFile")

            return LoadedConfigImpl(configRef, parsedContents, config.plugin)

        } catch (e: JsonMappingException) {
            throw RuntimeException("Error reading configuration file: " + configFile.absolutePath + ", reason: ${e.message}")
        } catch (e: IOException) {
            throw RuntimeException("Error reading configuration file: " + configFile.absolutePath, e)
        }
    }

    override fun <T : BasicPluginConfig> loadConfig(configFile: File, loadedConfig: LoadedConfig, configClass: Class<T>): T =
        ConfigUtil.lookupMapper(configFile).readValue(loadedConfig.serialised, configClass)!!
}
