/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.plugin.config.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a name/value pair, such as an HTTP header or query parameter,
 * with a logical operator controlling the match behaviour.
 *
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
                .collect(Collectors.toMap(Map.Entry::getKey, ConditionalNameValuePair::parsePair));
    }

    private static ConditionalNameValuePair parsePair(Map.Entry<String, Object> pair) {
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
