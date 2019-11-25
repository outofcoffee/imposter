package io.gatehill.imposter.util;

import io.gatehill.imposter.server.VertxWebServerFactoryImpl;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class HttpUtil {
    public static final String DEFAULT_SERVER_FACTORY = VertxWebServerFactoryImpl.class.getCanonicalName();

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String BIND_ALL_HOSTS = "0.0.0.0";
    public static final int DEFAULT_HTTP_LISTEN_PORT = 8080;
    public static final int DEFAULT_HTTPS_LISTEN_PORT = 8443;

    /* 2XX: generally "OK" */

    /**
     * HTTP Status-Code 200: OK.
     */
    public static final int HTTP_OK = 200;

    /**
     * HTTP Status-Code 201: Created.
     */
    public static final int HTTP_CREATED = 201;

    /**
     * HTTP Status-Code 202: Accepted.
     */
    public static final int HTTP_ACCEPTED = 202;

    /**
     * HTTP Status-Code 203: Non-Authoritative Information.
     */
    public static final int HTTP_NOT_AUTHORITATIVE = 203;

    /**
     * HTTP Status-Code 204: No Content.
     */
    public static final int HTTP_NO_CONTENT = 204;

    /**
     * HTTP Status-Code 205: Reset Content.
     */
    public static final int HTTP_RESET = 205;

    /**
     * HTTP Status-Code 206: Partial Content.
     */
    public static final int HTTP_PARTIAL = 206;

    /* 3XX: relocation/redirect */

    /**
     * HTTP Status-Code 300: Multiple Choices.
     */
    public static final int HTTP_MULT_CHOICE = 300;

    /**
     * HTTP Status-Code 301: Moved Permanently.
     */
    public static final int HTTP_MOVED_PERM = 301;

    /**
     * HTTP Status-Code 302: Temporary Redirect.
     */
    public static final int HTTP_MOVED_TEMP = 302;

    /**
     * HTTP Status-Code 303: See Other.
     */
    public static final int HTTP_SEE_OTHER = 303;

    /**
     * HTTP Status-Code 304: Not Modified.
     */
    public static final int HTTP_NOT_MODIFIED = 304;

    /**
     * HTTP Status-Code 305: Use Proxy.
     */
    public static final int HTTP_USE_PROXY = 305;

    /* 4XX: client error */

    /**
     * HTTP Status-Code 400: Bad Request.
     */
    public static final int HTTP_BAD_REQUEST = 400;

    /**
     * HTTP Status-Code 401: Unauthorized.
     */
    public static final int HTTP_UNAUTHORIZED = 401;

    /**
     * HTTP Status-Code 402: Payment Required.
     */
    public static final int HTTP_PAYMENT_REQUIRED = 402;

    /**
     * HTTP Status-Code 403: Forbidden.
     */
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * HTTP Status-Code 404: Not Found.
     */
    public static final int HTTP_NOT_FOUND = 404;

    /**
     * HTTP Status-Code 405: Method Not Allowed.
     */
    public static final int HTTP_BAD_METHOD = 405;

    /**
     * HTTP Status-Code 406: Not Acceptable.
     */
    public static final int HTTP_NOT_ACCEPTABLE = 406;

    /**
     * HTTP Status-Code 407: Proxy Authentication Required.
     */
    public static final int HTTP_PROXY_AUTH = 407;

    /**
     * HTTP Status-Code 408: Request Time-Out.
     */
    public static final int HTTP_CLIENT_TIMEOUT = 408;

    /**
     * HTTP Status-Code 409: Conflict.
     */
    public static final int HTTP_CONFLICT = 409;

    /**
     * HTTP Status-Code 410: Gone.
     */
    public static final int HTTP_GONE = 410;

    /**
     * HTTP Status-Code 411: Length Required.
     */
    public static final int HTTP_LENGTH_REQUIRED = 411;

    /**
     * HTTP Status-Code 412: Precondition Failed.
     */
    public static final int HTTP_PRECON_FAILED = 412;

    /**
     * HTTP Status-Code 413: Request Entity Too Large.
     */
    public static final int HTTP_ENTITY_TOO_LARGE = 413;

    /**
     * HTTP Status-Code 414: Request-URI Too Large.
     */
    public static final int HTTP_REQ_TOO_LONG = 414;

    /**
     * HTTP Status-Code 415: Unsupported Media Type.
     */
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /* 5XX: server error */

    /**
     * HTTP Status-Code 500: Internal Server Error.
     */
    public static final int HTTP_INTERNAL_ERROR = 500;

    /**
     * HTTP Status-Code 501: Not Implemented.
     */
    public static final int HTTP_NOT_IMPLEMENTED = 501;

    /**
     * HTTP Status-Code 502: Bad Gateway.
     */
    public static final int HTTP_BAD_GATEWAY = 502;

    /**
     * HTTP Status-Code 503: Service Unavailable.
     */
    public static final int HTTP_UNAVAILABLE = 503;

    /**
     * HTTP Status-Code 504: Gateway Timeout.
     */
    public static final int HTTP_GATEWAY_TIMEOUT = 504;

    /**
     * HTTP Status-Code 505: HTTP Version Not Supported.
     */
    public static final int HTTP_VERSION = 505;

    private HttpUtil() {
    }

    /**
     * Read the content types accepted by the requesting client, ordered by their weighting.
     *
     * @param routingContext the Vert.x routing context
     * @return the ordered content types
     */
    public static List<String> readAcceptedContentTypes(RoutingContext routingContext) {
        return readAcceptedContentTypes(routingContext.request().getHeader("Accept"));
    }

    /**
     * Read the content types accepted by the requesting client, ordered by their weighting.
     *
     * @param acceptHeader the value of the 'Accept' HTTP request header
     * @return the ordered content types
     */
    static List<String> readAcceptedContentTypes(String acceptHeader) {
        final List<WeightedAcceptEntry> accepts = newArrayList(ofNullable(acceptHeader)
                .map(a -> a.replaceAll("\\s*", ""))
                .map(a -> a.split(","))
                .orElse(new String[0])).parallelStream()
                .map(WeightedAcceptEntry::parse)
                .collect(Collectors.toList());

        // sort with highest at the top (descending order)
        accepts.sort((o1, o2) -> {
            if (null == o1 && null == o2) {
                return 0;
            }
            if (null == o1) {
                return -1;
            } else if (null == o2) {
                return 1;
            } else {
                return Float.compare(o2.weight, o1.weight);
            }
        });

        return accepts.parallelStream()
                .map(weightedAcceptEntry -> weightedAcceptEntry.contentType)
                .collect(Collectors.toList());
    }

    public static String buildStatusResponse() {
        return "{\n  \"status\":\"ok\"\n  \"version\":\"" + MetaUtil.readVersion() + "\"\n}";
    }

    private static class WeightedAcceptEntry {
        float weight;
        String contentType;

        static WeightedAcceptEntry parse(String weightAndContentType) {
            final WeightedAcceptEntry entry = new WeightedAcceptEntry();

            // split to obtain weight
            final String[] sEntry = weightAndContentType.split(";");
            entry.contentType = sEntry[0];
            entry.weight = (sEntry.length > 1 && sEntry[1].toLowerCase().startsWith("q=") ? Float.valueOf(sEntry[1].substring(2)) : 0);

            return entry;
        }
    }
}
