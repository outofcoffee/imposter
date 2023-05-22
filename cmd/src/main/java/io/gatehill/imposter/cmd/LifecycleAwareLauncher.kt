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
package io.gatehill.imposter.cmd

import io.gatehill.imposter.server.ImposterVerticle
import io.gatehill.imposter.util.FeatureUtil.doIfFeatureEnabled
import io.gatehill.imposter.util.MetricsUtil
import io.gatehill.imposter.util.MetricsUtil.configureMetrics
import io.vertx.core.Launcher
import io.vertx.core.VertxOptions
import io.vertx.core.impl.Utils
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteExisting

/**
 * @author Pete Cornish
 */
class LifecycleAwareLauncher : Launcher() {
    init {
        if (Utils.isWindows()) {
            // workaround for https://github.com/outofcoffee/imposter/issues/397
            val cacheDir = Files.createTempDirectory("vertx-cache").apply {
                deleteExisting()
            }
            System.setProperty("vertx.cacheDirBase", cacheDir.absolutePathString())
        }
    }

    override fun dispatch(args: Array<out String>) {
        super.dispatch(arrayOf("run", ImposterVerticle::class.java.canonicalName, *args))
    }

    override fun beforeStartingVertx(options: VertxOptions) = doIfFeatureEnabled(MetricsUtil.FEATURE_NAME_METRICS) {
        configureMetrics(options)
    }
}
