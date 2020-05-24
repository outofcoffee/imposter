package io.gatehill.imposter.script;

import io.vertx.core.MultiMap;

import static java.util.Optional.ofNullable;

/**
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
        private String method;
        private String uri;
        private String body;
        private MultiMap headers;

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
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public MultiMap getHeaders() {
            return headers;
        }

        public void setHeaders(MultiMap headers) {
            this.headers = headers;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "method='" + method + '\'' +
                    ", uri='" + uri + '\'' +
                    ", body=<" + ofNullable(body).map(b -> b.length() + " bytes").orElse("null") + '>' +
                    ", headers=" + headers +
                    '}';
        }
    }
}
