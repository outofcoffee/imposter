package com.gatehill.imposter.plugin.sfdc;

import com.gatehill.imposter.plugin.config.BaseConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginConfig extends BaseConfig {
    private String sObjectName;

    public String getsObjectName() {
        return sObjectName;
    }
}
