package com.gatehill.imposter.plugin.sfdc.config;

import com.gatehill.imposter.plugin.config.PluginConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginConfig extends PluginConfigImpl {
    private String sObjectName;

    public String getsObjectName() {
        return sObjectName;
    }
}
