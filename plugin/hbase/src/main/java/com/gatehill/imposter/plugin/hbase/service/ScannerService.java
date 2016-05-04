package com.gatehill.imposter.plugin.hbase.service;

import com.gatehill.imposter.plugin.hbase.HBasePluginConfig;
import com.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import com.gatehill.imposter.plugin.hbase.model.MockScanner;

import java.util.Optional;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScannerService {
    int registerScanner(HBasePluginConfig config, MockScanner scanner);

    Optional<InMemoryScanner> fetchScanner(int scannerId);

    void invalidateScanner(int scannerId);
}
