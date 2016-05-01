package com.gatehill.imposter.util;

import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class HttpUtil {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String BIND_ALL_HOSTS = "0.0.0.0";
    public static final String STATUS_RESPONSE = "{\"status\":\"ok\"}";

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
    public static List<String> readAcceptedContentTypes(String acceptHeader) {
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
