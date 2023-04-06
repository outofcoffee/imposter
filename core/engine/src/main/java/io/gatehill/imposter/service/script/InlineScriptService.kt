package io.gatehill.imposter.service.script

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.ResourceMatchResult
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.EvalResourceConfig
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager

class InlineScriptService {
    private val jsScriptService: ScriptService by lazy {
        InjectorUtil.getInstance<ScriptServiceFactory>().fetchScriptService("inline.js")
    }

    fun hasInlineScript(config: BasicResourceConfig): Boolean {
        return config is EvalResourceConfig && !config.eval.isNullOrBlank()
    }

    fun initScript(config: EvalResourceConfig) {
        if (config.eval.isNullOrBlank()) {
            return
        }
        jsScriptService.initInlineScript(config.resourceId, config.eval!!)
    }

    fun evalScript(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        config: BasicResourceConfig
    ): ResourceMatchResult {
        if (!hasInlineScript(config)) {
            // none configured
            return ResourceMatchResult.NO_CONFIG
        }
        config as EvalResourceConfig

        val scriptId = config.resourceId
        try {
            val executionContext = ScriptUtil.buildContext(httpExchange, emptyMap())
            val runtimeContext = RuntimeContext(
                emptyMap(),
                logger,
                pluginConfig,
                emptyMap(),
                executionContext
            )
            val result = jsScriptService.evalInlineScript(scriptId, config.eval!!, runtimeContext)
            if (logger.isTraceEnabled) {
                logger.trace("Evaluation of inline script {} result: {}: {}", scriptId, result, config.eval)
            } else {
                if (result) {
                    logger.debug("Inline script $scriptId evaluated to true for ${LogUtil.describeRequestShort(httpExchange)}")
                }
            }
            return if (result) ResourceMatchResult.EXACT_MATCH else ResourceMatchResult.NOT_MATCHED

        } catch (e: Exception) {
            logger.warn(
                "Error evaluating inline matcher script {} for ${LogUtil.describeRequestShort(httpExchange)} - treating as unmatched: {}",
                scriptId,
                config.eval,
                e
            )
            return ResourceMatchResult.NOT_MATCHED
        }
    }

    companion object {
        private val logger = LogManager.getLogger(InlineScriptService::class.java)
    }
}
