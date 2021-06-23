package io.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PerformanceSimulation {
    int exactDelayMs = -1;
    int minDelayMs = -1;
    int maxDelayMs = -1;

    public int getExactDelayMs() {
        return exactDelayMs;
    }

    public int getMinDelayMs() {
        return minDelayMs;
    }

    public int getMaxDelayMs() {
        return maxDelayMs;
    }
}
