package com.gatehill.imposter.plugin.hbase;

import com.gatehill.imposter.plugin.config.BaseConfig;
import org.apache.hadoop.hbase.rest.model.ScannerModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class MockScanner {
    private BaseConfig config;
    private ScannerModel scanner;
    private AtomicInteger rowCounter = new AtomicInteger();

    MockScanner(BaseConfig config, ScannerModel scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public BaseConfig getConfig() {
        return config;
    }

    public ScannerModel getScanner() {
        return scanner;
    }

    AtomicInteger getRowCounter() {
        return rowCounter;
    }
}
