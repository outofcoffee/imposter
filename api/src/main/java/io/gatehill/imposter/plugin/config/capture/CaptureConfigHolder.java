package io.gatehill.imposter.plugin.config.capture;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface CaptureConfigHolder {
    Map<String, CaptureConfig> getCaptureConfig();
}
