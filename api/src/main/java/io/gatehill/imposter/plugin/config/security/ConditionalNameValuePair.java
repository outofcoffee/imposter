package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ConditionalNameValuePair {
    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("operator")
    @SuppressWarnings("FieldMayBeFinal")
    private MatchOperator operator = MatchOperator.EqualTo;

    public ConditionalNameValuePair(String name, String value, MatchOperator operator) {
        this.name = name;
        this.value = value;
        this.operator = operator;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public MatchOperator getOperator() {
        return operator;
    }

    public static Map<String, ConditionalNameValuePair> parse(Map<String, Object> requestHeaders) {
        return requestHeaders.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, ConditionalNameValuePair::parseHttpHeaderMatch));
    }

    private static ConditionalNameValuePair parseHttpHeaderMatch(Map.Entry<String, Object> pair) {
        // String configuration form.
        // HeaderName: <value>
        if (pair.getValue() instanceof String) {
            return new ConditionalNameValuePair(pair.getKey(), (String) pair.getValue(), MatchOperator.EqualTo);
        }

        // Extended configuration form.
        // HeaderName:
        //   value: <value>
        //   operator: <operator>
        @SuppressWarnings("unchecked") final Map<String, String> structuredMatch = (Map<String, String>) pair.getValue();
        return new ConditionalNameValuePair(
                pair.getKey(),
                structuredMatch.get("value"),
                Enum.valueOf(MatchOperator.class, structuredMatch.get("operator"))
        );
    }
}
