package io.gatehill.imposter.plugin.hbase.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gatehill.imposter.plugin.hbase.config.HBasePluginConfig;
import io.gatehill.imposter.plugin.hbase.model.InMemoryScanner;
import io.gatehill.imposter.plugin.hbase.model.MockScanner;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ScannerServiceImpl implements ScannerService {
    /**
     * Hold scanners for a period of time.
     */
    private Cache<Integer, InMemoryScanner> createdScanners = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private AtomicInteger scannerIdCounter = new AtomicInteger();

    @Override
    public int registerScanner(HBasePluginConfig config, MockScanner scanner) {
        final int scannerId = scannerIdCounter.incrementAndGet();
        createdScanners.put(scannerId, new InMemoryScanner(config, scanner));
        return scannerId;
    }

    @Override
    public void invalidateScanner(int scannerId) {
        createdScanners.invalidate(scannerId);
    }

    @Override
    public Optional<InMemoryScanner> fetchScanner(int scannerId) {
        return ofNullable(createdScanners.getIfPresent(scannerId));
    }
}
