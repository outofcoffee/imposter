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
package io.gatehill.imposter.store.service

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.util.StoreUtil
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.buffer.Buffer
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookupFactory
import org.apache.logging.log4j.LogManager
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Resolves response template placeholders using stores.
 *
 * @author Pete Cornish
 */
class TemplateServiceImpl @Inject constructor(
    private val storeFactory: StoreFactory,
    private val expressionService: ExpressionService,
    engineLifecycle: EngineLifecycleHooks,
) : EngineLifecycleListener {

    /**
     * Whether to permit recursive interpreting/templating of untrusted data.
     */
    private val enableUnsafeTemplating: Boolean

    private val recursiveStoreItemSubstituter: StringSubstitutor
    private val nonrecursiveStoreItemSubstituter: StringSubstitutor

    init {
        recursiveStoreItemSubstituter = buildStoreItemSubstituter()
        nonrecursiveStoreItemSubstituter = buildStoreItemSubstituter().setDisableSubstitutionInValues(true)

        // disabled by default
        enableUnsafeTemplating = EnvVars.getEnv("IMPOSTER_UNTRUSTED_RECURSIVE_TEMPLATES")?.toBoolean() == true
        if (enableUnsafeTemplating) {
            LOGGER.warn("Warning: unsafe templating is enabled - this is a security risk if untrusted/unsanitised data is templated recursively")
        } else {
            LOGGER.trace("Recursive templating of untrusted data is disabled")
        }

        engineLifecycle.registerListener(this)
    }

    /**
     * @return a string substituter that replaces placeholders like 'example.foo' with the value of the
     * item 'foo' in the store 'example'.
     */
    private fun buildStoreItemSubstituter(): StringSubstitutor {
        val variableResolver = StringLookupFactory.INSTANCE.functionStringLookup { key: String ->
            try {
                val dotIndex = key.indexOf(".")
                if (dotIndex > 0) {
                    val storeName = key.substring(0, dotIndex)
                    val rawItemKey = key.substring(dotIndex + 1)
                    return@functionStringLookup expressionService.loadAndQuery(rawItemKey) { itemKey ->
                        val store = storeFactory.getStoreByName(storeName, false)
                        store.load<Any>(itemKey)
                    }
                }
                LOGGER.warn("Unknown store for template placeholder: $key")
                return@functionStringLookup ""

            } catch (e: Exception) {
                throw RuntimeException("Error replacing template placeholder '$key' with store item", e)
            }
        }
        return StringSubstitutor(variableResolver)
    }

    override fun beforeTransmittingTemplate(
        httpExchange: HttpExchange,
        responseData: Buffer,
        trustedData: Boolean,
    ): Buffer {
        if (responseData.length() == 0) {
            return responseData
        }

        // shim for request scoped store
        val uniqueRequestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
        val input = requestStorePrefixPattern
            .matcher(responseData.toString(Charsets.UTF_8))
            .replaceAll("\\$\\{" + StoreUtil.buildRequestStoreName(uniqueRequestId) + ".")

        val substitutor = determineSubstituter(trustedData)
        val transformed = substitutor.replace(input)

        return Buffer.buffer(transformed)
    }

    /**
     * Determine the substituter to use, depending on whether the
     * data is trusted and the value of [enableUnsafeTemplating].
     */
    private fun determineSubstituter(trustedData: Boolean): StringSubstitutor {
        if (trustedData) {
            return recursiveStoreItemSubstituter
        } else {
            // do not permit recursive interpolation of untrusted data by default, for security reasons
            if (enableUnsafeTemplating) {
                LOGGER.warn("Warning: recursive templating of untrusted data is enabled. Templating untrusted data is a security risk!")
                return recursiveStoreItemSubstituter
            } else {
                return nonrecursiveStoreItemSubstituter
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(TemplateServiceImpl::class.java)
        private val requestStorePrefixPattern = Pattern.compile("\\$\\{" + StoreUtil.REQUEST_SCOPED_STORE_NAME + "\\.")
    }
}
