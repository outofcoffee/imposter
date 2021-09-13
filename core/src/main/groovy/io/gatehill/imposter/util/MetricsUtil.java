package io.gatehill.imposter.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
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
}
