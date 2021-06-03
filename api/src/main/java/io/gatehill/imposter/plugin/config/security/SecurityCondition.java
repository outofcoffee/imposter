package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityCondition {
    @JsonProperty("effect")
    @SuppressWarnings("FieldMayBeFinal")
    private SecurityEffect effect = SecurityEffect.Deny;

    @JsonProperty("requestHeaders")
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, Object> requestHeaders = emptyMap();

    @JsonIgnore
    private List<HttpHeader> parsedHeaders;

    public SecurityEffect getEffect() {
        return effect;
    }

    public Map<String, Object> getRequestHeaders() {
        return requestHeaders;
    }

    public List<HttpHeader> getParsedHeaders() {
        if (isNull(parsedHeaders)) {
            parsedHeaders = requestHeaders.entrySet().stream()
                    .map(this::parseHttpHeaderMatch)
                    .collect(Collectors.toList());
        }
        return parsedHeaders;
    }

    private HttpHeader parseHttpHeaderMatch(Map.Entry<String, Object> h) {
        if (h.getValue() instanceof Map) {
            @SuppressWarnings("unchecked") final Map<String, String> structuredMatch = (Map<String, String>) h.getValue();
            return new HttpHeader(
                    h.getKey(),
                    structuredMatch.get("value"),
                    Enum.valueOf(MatchOperator.class, structuredMatch.get("operator"))
            );
        }
        return new HttpHeader(h.getKey(), (String) h.getValue(), MatchOperator.EqualTo);
    }
}
