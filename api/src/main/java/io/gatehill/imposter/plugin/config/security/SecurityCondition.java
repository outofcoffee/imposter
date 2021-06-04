package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private Map<String, HttpHeader> parsedHeaders;

    public SecurityEffect getEffect() {
        return effect;
    }

    public Map<String, Object> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, HttpHeader> getParsedHeaders() {
        if (isNull(parsedHeaders)) {
            parsedHeaders = requestHeaders.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, this::parseHttpHeaderMatch));
        }
        return parsedHeaders;
    }

    private HttpHeader parseHttpHeaderMatch(Map.Entry<String, Object> h) {
        // String configuration form.
        // Header-Name: <value>
        if (h.getValue() instanceof String) {
            return new HttpHeader(h.getKey(), (String) h.getValue(), MatchOperator.EqualTo);
        }

        // Extended configuration form.
        // Header-Name:
        //   value: <value>
        //   operator: <operator>
        @SuppressWarnings("unchecked") final Map<String, String> structuredMatch = (Map<String, String>) h.getValue();
        return new HttpHeader(
                h.getKey(),
                structuredMatch.get("value"),
                Enum.valueOf(MatchOperator.class, structuredMatch.get("operator"))
        );
    }
}
