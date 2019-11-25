package io.gatehill.imposter.plugin.hbase.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResultCell {
    private final String fieldName;
    private final String fieldValue;

    public ResultCell(String fieldName, String fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
