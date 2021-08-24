package io.gatehill.imposter.plugin.config.resource;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig;
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyResourceConfig;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestResourceConfig extends AbstractResourceConfig implements
        MethodResourceConfig, PathParamsResourceConfig, QueryParamsResourceConfig, LegacyQueryParamsResourceConfig, RequestHeadersResourceConfig, RequestBodyResourceConfig {

    @JsonProperty("method")
    private ResourceMethod method;

    @JsonProperty("pathParams")
    private Map<String, String> pathParams;

    @JsonProperty("queryParams")
    @JsonAlias("params")
    private Map<String, String> queryParams;

    @JsonProperty("requestHeaders")
    private Map<String, String> requestHeaders;

    @JsonProperty("requestBody")
    private RequestBodyConfig requestBody;

    @Override
    public Map<String, String> getPathParams() {
        return pathParams;
    }

    @Override
    public Map<String, String> getParams() {
        return getQueryParams();
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public ResourceMethod getMethod() {
        return method;
    }

    @Override
    public RequestBodyConfig getRequestBody() {
        return requestBody;
    }
}
