/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.service

import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.model.steps.PreparedStep
import io.gatehill.imposter.model.steps.RemoteProcessingStep
import io.gatehill.imposter.model.steps.RemoteStepConfig
import io.gatehill.imposter.model.steps.RemoteStepContext
import io.gatehill.imposter.model.steps.ScriptProcessingStep
import io.gatehill.imposter.model.steps.ScriptStepConfig
import io.gatehill.imposter.model.steps.ScriptStepContext
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.steps.StepConfig
import io.gatehill.imposter.plugin.config.steps.StepType
import io.gatehill.imposter.plugin.config.steps.StepsConfigHolder
import io.gatehill.imposter.util.MapUtil
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * Parses processing steps.
 */
class StepService @Inject constructor(
    private val scriptedResponseService: ScriptedResponseService,
    private val remoteService: RemoteService,
    private val captureService: CaptureService,
) {
    private val logger = LogManager.getLogger(StepService::class.java)

    private val remoteStepImpl by lazy { RemoteProcessingStep(remoteService, captureService) }
    private val scriptStepImpl by lazy { ScriptProcessingStep(scriptedResponseService) }

    private val stepCache = CacheBuilder.newBuilder().maximumSize(40).build<String, List<PreparedStep>>()

    /**
     * Parses the steps for the given resource.
     */
    fun determineSteps(
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig,
    ): List<PreparedStep> {
        val steps = stepCache.get(resourceConfig.resourceId) {
            prepareSteps(pluginConfig, resourceConfig)
        }
        logger.trace("Prepared {} step(s) for resource with ID: {}: {}", steps.size, resourceConfig.resourceId, resourceConfig)
        return steps
    }

    private fun prepareSteps(
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig,
    ): List<PreparedStep> {
        val steps = mutableListOf<PreparedStep>()

        if (resourceConfig is StepsConfigHolder) {
            resourceConfig.steps?.let { steps += parseSteps(it, pluginConfig, resourceConfig) }
        }

        // convert explicit script file to step
        // this covers a 'scriptFile` set on the root plugin config
        getExplicitScriptFile(resourceConfig, pluginConfig)?.let { scriptFile ->
            val stepId = "${resourceConfig.resourceId}_scriptFile"
            steps += parseScriptStep(resourceConfig, stepId, ScriptStepConfig(scriptFile = scriptFile), pluginConfig)
        }
        return steps
    }

    private fun parseSteps(
        steps: List<StepConfig>,
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig,
    ): List<PreparedStep> {
        return steps.mapIndexed { index, step ->
            // unique combination of resource ID and step index
            val stepId = "${resourceConfig.resourceId}_step$index"
            when (step.type) {
                StepType.Remote -> parseRemoteStep(resourceConfig, stepId, step)
                StepType.Script -> parseScriptStep(resourceConfig, stepId, step, pluginConfig)
                else -> throw IllegalStateException("Unsupported step type: ${step.type}")
            }
        }
    }

    private fun parseRemoteStep(resourceConfig: BasicResourceConfig, stepId: String, step: StepConfig): PreparedStep {
        val config = MapUtil.converter.convertValue(step, RemoteStepConfig::class.java)
        return PreparedStep(
            type = StepType.Remote,
            step = remoteStepImpl,
            context = RemoteStepContext(stepId, resourceConfig, config),
        )
    }

    private fun parseScriptStep(
        resourceConfig: BasicResourceConfig,
        stepId: String,
        step: StepConfig,
        pluginConfig: PluginConfig,
    ): PreparedStep {
        val config = MapUtil.converter.convertValue(step, ScriptStepConfig::class.java)
        return parseScriptStep(resourceConfig, stepId, config, pluginConfig)
    }

    private fun parseScriptStep(
        resourceConfig: BasicResourceConfig,
        stepId: String,
        config: ScriptStepConfig,
        pluginConfig: PluginConfig,
    ) = PreparedStep(
        type = StepType.Script,
        step = scriptStepImpl,
        context = ScriptStepContext(stepId, resourceConfig, config, pluginConfig)
    )

    private fun getExplicitScriptFile(
        resourceConfig: BasicResourceConfig,
        pluginConfig: PluginConfig,
    ): String? {
        val scriptFile: String? = resourceConfig.responseConfig.scriptFile ?: run {
            val inheritScriptFile = pluginConfig is ResourcesHolder<*> && pluginConfig.isDefaultsFromRootResponse == true
            if (inheritScriptFile && pluginConfig is BasicResourceConfig) {
                logger.trace("Inheriting root script file configuration as defaults")
                pluginConfig.responseConfig.scriptFile
            } else {
                null
            }
        }
        return scriptFile?.takeIf { it.isNotEmpty() }
    }
}
