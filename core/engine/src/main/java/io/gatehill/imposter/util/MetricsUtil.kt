/*
 * Copyright (c) 2016-2024.
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
package io.gatehill.imposter.util

import io.micrometer.common.util.internal.logging.InternalLoggerFactory
import io.micrometer.common.util.internal.logging.Log4J2LoggerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vertx.core.VertxOptions
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.backends.BackendRegistries
import org.apache.logging.log4j.LogManager
import java.util.function.Consumer

/**
 * @author Pete Cornish
 */
object MetricsUtil {
    private val LOGGER = LogManager.getLogger(MetricsUtil::class.java)
    const val FEATURE_NAME_METRICS = "metrics"

    @JvmStatic
    fun configureMetrics(options: VertxOptions): VertxOptions {
        // Micrometer uses SLF4J 1.7, which is incompatible with other libraries
        // that use SLF4J 2, so we need to force it to use Log4J2
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

        return options.setMetricsOptions(
            MicrometerMetricsOptions()
                .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                .setJvmMetricsEnabled(true)
                .setEnabled(true)
        )
    }

    @JvmStatic
    fun doIfMetricsEnabled(description: String?, block: Consumer<MeterRegistry>): ChainableMetricsStarter {
        return if (FeatureUtil.isFeatureEnabled(FEATURE_NAME_METRICS)) {
            BackendRegistries.getDefaultNow()?.let { registry ->
                block.accept(registry)
                ChainableMetricsStarter(true)
            } ?: run {
                // this is important to avoid NPEs if we are running in a context, such as embedded,
                // in which metrics are not explicitly disabled, but are not initialised
                LOGGER.warn("No metrics registry - skipping {}", description)
                ChainableMetricsStarter(false)
            }
        } else {
            LOGGER.trace("Metrics disabled - skipping {}", description)
            ChainableMetricsStarter(false)
        }
    }

    class ChainableMetricsStarter internal constructor(private val primaryCondition: Boolean) {
        fun orElseDo(block: Runnable) {
            if (!primaryCondition) {
                block.run()
            }
        }
    }
}