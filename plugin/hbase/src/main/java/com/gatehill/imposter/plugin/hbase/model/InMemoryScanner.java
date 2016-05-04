package com.gatehill.imposter.plugin.hbase.model;

import com.gatehill.imposter.plugin.config.BaseConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryScanner {
    private BaseConfig config;
    private MockScanner scanner;
    private AtomicInteger rowCounter = new AtomicInteger();

    public InMemoryScanner(BaseConfig config, MockScanner scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public BaseConfig getConfig() {
        return config;
    }

    public MockScanner getScanner() {
        return scanner;
    }

    public AtomicInteger getRowCounter() {
        return rowCounter;
    }
}
