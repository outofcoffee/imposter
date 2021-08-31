package io.gatehill.imposter.server.util;

import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class MetricsUtil {
    private MetricsUtil() {
    }

    public static VertxOptions configureMetrics(VertxOptions options) {
        return options.setMetricsOptions(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
                .setJvmMetricsEnabled(true)
                .setEnabled(true)
        );
    }
}
