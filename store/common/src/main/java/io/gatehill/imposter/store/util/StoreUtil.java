package io.gatehill.imposter.store.util;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class StoreUtil {
    public static final String REQUEST_SCOPED_STORE_NAME = "request";

    private StoreUtil() {
    }

    public static boolean isRequestScopedStore(String storeName) {
        return REQUEST_SCOPED_STORE_NAME.equals(storeName);
    }

    public static String buildRequestStoreName(String requestId) {
        return "request_" + requestId;
    }
}
