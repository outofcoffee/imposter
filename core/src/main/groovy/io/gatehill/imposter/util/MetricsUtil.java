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

package io.gatehill.imposter.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MetricsUtil {
    private static final Logger LOGGER = LogManager.getLogger(MetricsUtil.class);
    public static final String FEATURE_NAME_METRICS = "metrics";

    private MetricsUtil() {
    }

    public static VertxOptions configureMetrics(VertxOptions options) {
        return options.setMetricsOptions(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
                .setJvmMetricsEnabled(true)
                .setEnabled(true)
        );
    }

    public static ChainableMetricsStarter doIfMetricsEnabled(String description, Consumer<MeterRegistry> block) {
        if (FeatureUtil.isFeatureEnabled(FEATURE_NAME_METRICS)) {
            final MeterRegistry registry = BackendRegistries.getDefaultNow();
            if (nonNull(registry)) {
                block.accept(registry);
                return new ChainableMetricsStarter(true);
            } else {
                // this is important to avoid NPEs if we are running in a context, such as embedded,
                // in which metrics are not explicitly disabled, but are not initialised
                LOGGER.warn("No metrics registry - skipping {}", description);
                return new ChainableMetricsStarter(false);
            }
        } else {
            LOGGER.debug("Metrics disabled - skipping {}", description);
            return new ChainableMetricsStarter(false);
        }
    }

    public static class ChainableMetricsStarter {
        final boolean primaryCondition;

        ChainableMetricsStarter(boolean primaryCondition) {
            this.primaryCondition = primaryCondition;
        }

        public void orElseDo(Runnable block) {
            if (!primaryCondition) {
                block.run();
            }
        }
    }

    public static Handler<RoutingContext> createHandler() {
        return PrometheusScrapingHandler.create();
    }
}
