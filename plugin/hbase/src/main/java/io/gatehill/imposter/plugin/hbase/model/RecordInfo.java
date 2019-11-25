package io.gatehill.imposter.plugin.hbase.model;

public class RecordInfo {
    private String recordId;

    public RecordInfo(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
}
