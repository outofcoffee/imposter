package com.gatehill.imposter.plugin.hbase.model;

import com.gatehill.imposter.plugin.config.BaseConfig;
import org.apache.hadoop.hbase.rest.model.ScannerModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class MockScanner {
    private BaseConfig config;
    private ScannerModel scanner;
    private AtomicInteger rowCounter = new AtomicInteger();

    public MockScanner(BaseConfig config, ScannerModel scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public BaseConfig getConfig() {
        return config;
    }

    public ScannerModel getScanner() {
        return scanner;
    }

    public AtomicInteger getRowCounter() {
        return rowCounter;
    }
}
