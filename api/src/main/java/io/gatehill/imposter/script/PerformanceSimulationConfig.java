package io.gatehill.imposter.script;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PerformanceSimulationConfig {
    @JsonProperty("exact")
    private Integer exactDelayMs;

    @JsonProperty("min")
    private Integer minDelayMs;

    @JsonProperty("max")
    private Integer maxDelayMs;

    public void setExactDelayMs(Integer exactDelayMs) {
        this.exactDelayMs = exactDelayMs;
    }

    public Integer getExactDelayMs() {
        return exactDelayMs;
    }

    public void setMinDelayMs(Integer minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public Integer getMinDelayMs() {
        return minDelayMs;
    }

    public void setMaxDelayMs(Integer maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public Integer getMaxDelayMs() {
        return maxDelayMs;
    }
}
