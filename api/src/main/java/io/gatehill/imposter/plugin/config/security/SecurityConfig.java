package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SecurityConfig {
    @JsonProperty("default")
    @SuppressWarnings("FieldMayBeFinal")
    private SecurityEffect defaultEffect = SecurityEffect.Deny;

    @JsonProperty("conditions")
    @SuppressWarnings("FieldMayBeFinal")
    private List<SecurityCondition> conditions = emptyList();

    public SecurityEffect getDefaultEffect() {
        return defaultEffect;
    }

    public List<SecurityCondition> getConditions() {
        return conditions;
    }
}
