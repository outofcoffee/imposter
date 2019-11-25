package io.gatehill.imposter.plugin.hbase.config;

import io.gatehill.imposter.plugin.config.PluginConfigImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HBasePluginConfig extends PluginConfigImpl {
    private String tableName;
    private String prefix;
    private String idField;

    public String getTableName() {
        return tableName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getIdField() {
        return idField;
    }
}
