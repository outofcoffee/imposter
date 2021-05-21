package io.gatehill.imposter.script;

import java.util.Map;

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
        private String path;
        private String method;
        private String uri;
        private String body;
        private Map<String, String> headers;
        private Map<String, String> params;

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
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "path='" + path + '\'' +
                    ", method='" + method + '\'' +
                    ", uri='" + uri + '\'' +
                    ", body=<" + ofNullable(body).map(b -> b.length() + " bytes").orElse("null") + '>' +
                    ", headers=" + headers +
                    ", params=" + params +
                    '}';
        }
    }
}
