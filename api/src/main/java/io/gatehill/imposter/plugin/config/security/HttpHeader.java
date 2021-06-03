package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpHeader {
    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("operator")
    @SuppressWarnings("FieldMayBeFinal")
    private MatchOperator operator = MatchOperator.EqualTo;

    public HttpHeader(String name, String value, MatchOperator operator) {
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
}
