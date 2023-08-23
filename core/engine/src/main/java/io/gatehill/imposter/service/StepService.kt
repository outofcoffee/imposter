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
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.model.steps.PreparedStep
import io.gatehill.imposter.model.steps.RemoteProcessingStep
import io.gatehill.imposter.model.steps.RemoteStepContext
import io.gatehill.imposter.model.steps.ScriptProcessingStep
import io.gatehill.imposter.model.steps.ScriptStepContext
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.steps.StepConfig
import io.gatehill.imposter.plugin.config.steps.StepsConfigHolder
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
        getExplicitScriptFile(resourceConfig, pluginConfig)?.let { scriptFile ->
            val stepId = "${resourceConfig.resourceId}_scriptFile"
            steps += parseScriptStep(stepId, null, scriptFile, pluginConfig)
        }
        return steps
    }

    private fun parseSteps(
        steps: List<StepConfig>,
        pluginConfig: PluginConfig,
        resourceConfig: StepsConfigHolder,
    ): List<PreparedStep> {
        return steps.mapIndexed { index, step ->
            // unique combination of resource ID and step index
            val stepId = "${resourceConfig.resourceId}_step$index"
            when (step.type) {
                "remote" -> parseRemoteStep(stepId, step)
                "script" -> parseScriptStep(stepId, step, pluginConfig)
                else -> throw IllegalStateException("Unsupported step type: ${step.type}")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRemoteStep(stepId: String, step: Map<String, *>): PreparedStep {
        val capture = step["capture"] as Map<String, Map<String, *>>?
        val captureConfig: Map<String, ItemCaptureConfig>? = capture?.let { configs ->
            configs.mapValues { (_, config) ->
                // only a subset of the capture config is supported for remote steps
                val expression = config["expression"] as String?
                ItemCaptureConfig(
                    // assumes store is a string
                    _store = config["store"],
                    // syntactic sugar for remote context steps
                    expression = expression?.replace("\${remote.", "\${context."),
                )
            }
        }
        return PreparedStep(
            step = remoteStepImpl,
            context = RemoteStepContext(
                stepId = stepId,
                url = step["url"] as String,
                method = HttpMethod.valueOf(step["method"] as String),
                queryParams = step["queryParams"] as Map<String, String>?,
                formParams = step["formParams"] as Map<String, String>?,
                headers = step["headers"] as Map<String, String>?,
                content = step["content"] as String?,
                capture = captureConfig,
            ),
        )
    }

    private fun parseScriptStep(
        stepId: String,
        step: Map<String, *>,
        pluginConfig: PluginConfig,
    ) = parseScriptStep(
        stepId,
        step["code"] as String?,
        step["scriptFile"] as String?,
        pluginConfig,
    )

    private fun parseScriptStep(
        stepId: String,
        scriptCode: String?,
        scriptFile: String?,
        pluginConfig: PluginConfig,
    ) = PreparedStep(
        step = scriptStepImpl,
        context = ScriptStepContext(
            stepId,
            pluginConfig,
            scriptCode,
            scriptFile,
        )
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
