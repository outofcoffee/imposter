package io.gatehill.imposter.plugin.openapi.util;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ValidationReportUtil {
    private ValidationReportUtil() {
    }

    public static void sendValidationReport(RoutingContext routingContext, String reportMessages) {
        if (routingContext.parsedHeaders().accept().stream().anyMatch(a -> a.rawValue().equals("text/html"))) {
            routingContext.response().putHeader("Content-Type", "text/html")
                    .end(buildResponseReportHtml(reportMessages));
        } else {
            routingContext.response().putHeader("Content-Type", "text/plain")
                    .end(buildResponseReportPlain(reportMessages));
        }
    }

    private static String buildResponseReportPlain(String reportMessages) {
        return "Request validation failed:\n" + reportMessages + "\n";
    }

    private static String buildResponseReportHtml(String reportMessages) {
        return "<html>\n" +
                "<head><title>Invalid request</title></head>\n" +
                "<body><h1>Request validation failed</h1><br/><pre>" + reportMessages + "</pre></body>\n" +
                "</html>\n";
    }
}
