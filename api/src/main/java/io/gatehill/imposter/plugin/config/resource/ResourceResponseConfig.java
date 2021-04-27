package io.gatehill.imposter.plugin.config.resource;

/**
 * Represents a response configuration for a given RESTful resource.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResourceResponseConfig extends RestResourceConfig {
    private Integer statusCode;

    public Integer getStatusCode() {
        return statusCode;
    }
}
