package com.gatehill.imposter.plugin.hbase;

import com.gatehill.imposter.plugin.config.BaseConfig;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HBasePluginConfig extends BaseConfig {
    private String tableName;
    private String prefix;

    public String getTableName() {
        return tableName;
    }

    public String getPrefix() {
        return prefix;
    }
}
