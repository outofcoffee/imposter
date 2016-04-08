package com.gatehill.imposter.plugin.sfdc;

import com.gatehill.imposter.plugin.rest.RestPluginConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginConfig extends RestPluginConfig {
    private String sObjectName;

    public SfdcPluginConfig() {
        this.contentType = "application/json";
    }

    public String getsObjectName() {
        return sObjectName;
    }
}
