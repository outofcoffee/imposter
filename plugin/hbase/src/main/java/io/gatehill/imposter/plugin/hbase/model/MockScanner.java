package io.gatehill.imposter.plugin.hbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockScanner {
    private String filter;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
