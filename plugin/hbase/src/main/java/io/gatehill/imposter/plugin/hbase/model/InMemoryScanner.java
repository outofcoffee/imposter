package io.gatehill.imposter.plugin.hbase.model;

import io.gatehill.imposter.plugin.config.PluginConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryScanner {
    private PluginConfig config;
    private MockScanner scanner;
    private AtomicInteger rowCounter = new AtomicInteger();

    public InMemoryScanner(PluginConfig config, MockScanner scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public MockScanner getScanner() {
        return scanner;
    }

    public AtomicInteger getRowCounter() {
        return rowCounter;
    }
}
