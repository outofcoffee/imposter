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
package io.gatehill.imposter.service

import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.scripting.AbstractBaseScriptTest
import io.gatehill.imposter.scripting.graalvm.service.GraalvmScriptServiceImpl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import javax.inject.Inject

/**
 * Verify that scripts can access additional properties of the context.
 *
 * @author Pete Cornish
 */
class AdditionalContextPropertiesTest : AbstractBaseScriptTest() {
    @Inject
    private lateinit var service: GraalvmScriptServiceImpl

    override fun getService() = service

    override fun getScriptName() = "additional_context_properties.js"

    @Test
    fun `access properties of context item`() {
        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as BasicResourceConfig

        val scriptBindings = buildScriptBindings(
            additionalContext = mapOf(
                "additional" to ParentItem()
            )
        )
        val script = resolveScriptFile(pluginConfig, resourceConfig)
        val actual = getService().executeScript(script, scriptBindings)

        val content = actual.content
        assertThat(content, equalTo("foo bar"))
    }

    class ParentItem() {
        val name = "foo"
        val child = ChildItem()
    }

    class ChildItem() {
        val name = "bar"
    }
}
