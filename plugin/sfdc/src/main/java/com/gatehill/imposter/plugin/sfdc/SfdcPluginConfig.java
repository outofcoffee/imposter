package com.gatehill.imposter.plugin.sfdc;

import com.gatehill.imposter.plugin.rest.RestPluginConfig;

import static com.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginConfig extends RestPluginConfig {
    private String sObjectName;

    public SfdcPluginConfig() {
        this.contentType = CONTENT_TYPE_JSON;
    }

    public String getsObjectName() {
        return sObjectName;
    }
}
