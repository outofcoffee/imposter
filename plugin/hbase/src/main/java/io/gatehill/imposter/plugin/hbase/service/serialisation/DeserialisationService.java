package io.gatehill.imposter.plugin.hbase.service.serialisation;

import io.gatehill.imposter.plugin.hbase.model.MockScanner;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface DeserialisationService {
    /**
     * @param routingContext the Vert.x routing context
     * @return the scanner
     */
    MockScanner decodeScanner(RoutingContext routingContext);

    /**
     * @param scanner the scanner from which to read the filter
     * @return the scanner filter prefix
     */
    Optional<String> decodeScannerFilterPrefix(MockScanner scanner);
}
