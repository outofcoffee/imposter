package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig;
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestResourceConfig extends AbstractResourceConfig implements MethodResourceConfig, RequestBodyResourceConfig {
    private ResourceMethod method;

    @JsonProperty("requestBody")
    private RequestBodyConfig requestBody;

    @Override
    public ResourceMethod getMethod() {
        return method;
    }

    @Override
    public RequestBodyConfig getRequestBody() {
        return requestBody;
    }
}
