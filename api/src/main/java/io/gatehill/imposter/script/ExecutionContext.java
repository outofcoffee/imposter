package io.gatehill.imposter.script;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * Representation of the request, supporting lazily-initialised collections for params and headers.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ExecutionContext {
    private Request request;

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "request=" + request +
                '}';
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public static class Request {
        private String path;
        private String method;
        private String uri;

        private final Supplier<Map<String, String>> headersSupplier;
        private Map<String, String> headers;

        private final Supplier<Map<String, String>> pathParamsSupplier;
        private Map<String, String> pathParams;

        private final Supplier<Map<String, String>> queryParamsSupplier;
        private Map<String, String> queryParams;

        private final Supplier<String> bodySupplier;
        private String body;

        public Request(
                Supplier<Map<String, String>> headersSupplier,
                Supplier<Map<String, String>> pathParamsSupplier,
                Supplier<Map<String, String>> queryParamsSupplier,
                Supplier<String> bodySupplier
        ) {
            this.headersSupplier = headersSupplier;
            this.pathParamsSupplier = pathParamsSupplier;
            this.queryParamsSupplier = queryParamsSupplier;
            this.bodySupplier = bodySupplier;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getBody() {
            if (isNull(body)) {
                body = bodySupplier.get();
            }
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            if (isNull(headers)) {
                headers = headersSupplier.get();
            }
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        /**
         * @return the request path parameters
         */
        public Map<String, String> getPathParams() {
            if (isNull(pathParams)) {
                pathParams = pathParamsSupplier.get();
            }
            return pathParams;
        }

        public void setPathParams(Map<String, String> pathParams) {
            this.pathParams = pathParams;
        }

        /**
         * @return the request query parameters
         */
        public Map<String, String> getQueryParams() {
            if (isNull(queryParams)) {
                queryParams = queryParamsSupplier.get();
            }
            return queryParams;
        }

        public void setQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        /**
         * Use {@link #getQueryParams()} instead.
         *
         * @return the request query parameters
         */
        @Deprecated
        public Map<String, String> getParams() {
            return getQueryParams();
        }

        @Override
        public String toString() {
            return "Request{" +
                    "path='" + path + '\'' +
                    ", method='" + method + '\'' +
                    ", uri='" + uri + '\'' +
                    ", pathParams=" + getPathParams() +
                    ", queryParams=" + getQueryParams() +
                    ", headers=" + getHeaders() +
                    ", body=<" + ofNullable(getBody()).map(b -> b.length() + " bytes").orElse("null") + '>' +
                    '}';
        }
    }
}
