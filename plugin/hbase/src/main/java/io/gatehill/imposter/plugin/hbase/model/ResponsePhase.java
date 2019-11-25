package io.gatehill.imposter.plugin.hbase.model;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public enum ResponsePhase {
    /**
     * Creation of a scanner.
     */
    SCANNER,

    /**
     * Read results from a scanner.
     */
    RESULTS,

    /**
     * Fetch a single record.
     */
    RECORD
}
