package io.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface MutableResponseBehaviour {
    MutableResponseBehaviour withHeader(String header, String value);

    MutableResponseBehaviour withStatusCode(int statusCode);

    MutableResponseBehaviour withFile(String responseFile);

    MutableResponseBehaviour withEmpty();

    MutableResponseBehaviour withData(String responseData);

    MutableResponseBehaviour withExampleName(String exampleName);

    MutableResponseBehaviour usingDefaultBehaviour();

    MutableResponseBehaviour skipDefaultBehaviour();

    /**
     * @deprecated use {@link #skipDefaultBehaviour()} instead
     * @return this
     */
    @Deprecated
    MutableResponseBehaviour immediately();

    MutableResponseBehaviour respond();

    MutableResponseBehaviour respond(Runnable closure);

    MutableResponseBehaviour and();

    MutableResponseBehaviour withPerformance(PerformanceSimulationConfig performance);

    MutableResponseBehaviour withDelay(int exactDelayMs);

    MutableResponseBehaviour withDelayRange(int minDelayMs, int maxDelayMs);
}
