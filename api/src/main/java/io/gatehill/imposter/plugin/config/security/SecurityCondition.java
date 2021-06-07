package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityCondition {
    @JsonProperty("effect")
    @SuppressWarnings("FieldMayBeFinal")
    private SecurityEffect effect = SecurityEffect.Deny;

    /**
     * Raw configuration. Use {@link #getQueryParams()} instead.
     */
    @JsonProperty("queryParams")
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, Object> queryParams = emptyMap();

    /**
     * Raw configuration. Use {@link #getRequestHeaders()} instead.
     */
    @JsonProperty("requestHeaders")
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, Object> requestHeaders = emptyMap();

    @JsonIgnore
    private Map<String, ConditionalNameValuePair> parsedQueryParams;

    @JsonIgnore
    private Map<String, ConditionalNameValuePair> parsedHeaders;

    public SecurityEffect getEffect() {
        return effect;
    }

    public Map<String, ConditionalNameValuePair> getQueryParams() {
        if (isNull(parsedQueryParams)) {
            parsedQueryParams = ConditionalNameValuePair.parse(queryParams);
        }
        return parsedQueryParams;
    }

    public Map<String, ConditionalNameValuePair> getRequestHeaders() {
        if (isNull(parsedHeaders)) {
            parsedHeaders = ConditionalNameValuePair.parse(requestHeaders);
        }
        return parsedHeaders;
    }
}
