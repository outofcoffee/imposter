package com.gatehill.imposter;

import org.apache.hadoop.hbase.rest.model.ScannerModel;

/**
 * @author pcornish
 */
public class MockScanner {
    private BaseMockConfig config;
    private ScannerModel scanner;

    public MockScanner(BaseMockConfig config, ScannerModel scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public BaseMockConfig getConfig() {
        return config;
    }

    public ScannerModel getScanner() {
        return scanner;
    }
}
