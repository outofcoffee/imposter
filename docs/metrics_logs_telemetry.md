# Metrics, health, logs and telemetry

## Status endpoint

Imposter exposes a status endpoint under `/system/status`

This is useful as a healthcheck endpoint, such as for liveness or readiness checks.

```shell
$ curl http://localhost:8080/system/status

{
  "status":"ok",
  "version":"1.20.0"
}
```

## Metrics

Imposter exposes telemetry using Prometheus under `/system/metrics`

This enables you to examine track various metrics, such as response time, error rates, total request count etc.

```shell
$ curl http://localhost:8080/system/metrics
    
# HELP vertx_http_server_bytesReceived Number of bytes received by the server
# TYPE vertx_http_server_bytesReceived summary
vertx_http_server_bytesReceived_count 6.0

# HELP vertx_http_server_requestCount_total Number of processed requests
# TYPE vertx_http_server_requestCount_total counter
vertx_http_server_requestCount_total{code="200",method="GET",} 5.0

# HELP vertx_http_server_connections Number of opened connections to the server
# TYPE vertx_http_server_connections gauge
vertx_http_server_connections 2.0

# HELP vertx_http_server_responseTime_seconds Request processing time
# TYPE vertx_http_server_responseTime_seconds summary
vertx_http_server_responseTime_seconds_count{code="200",method="GET",} 5.0
vertx_http_server_responseTime_seconds_sum{code="200",method="GET",} 0.1405811

# HELP vertx_http_server_responseTime_seconds_max Request processing time
# TYPE vertx_http_server_responseTime_seconds_max gauge
vertx_http_server_responseTime_seconds_max{code="200",method="GET",} 0.1039024
```

For example, to calculate the average response time, use the following PromQL:

    vertx_http_server_responseTime_seconds_sum / vertx_http_server_responseTime_seconds_count

Other useful metrics:

| Metric name                 | Purpose                                     |
|-----------------------------|---------------------------------------------|
| response_file_cache_entries | The number of cached response files         |
| script_execution_duration   | Script engine execution duration in seconds |
| script_cache_entries        | The number of cached compiled scripts       |

> Also see [the metrics example](https://github.com/imposter-project/examples/blob/main/metrics).

## Logs

Logs are printed to stdout.

You can control the logging level using the following environment variable:
    
    # also supports WARN, INFO, DEBUG etc.
    export IMPOSTER_LOG_LEVEL="TRACE"

Internally, Log4J2 is used, so the usual configuration options apply.

### Structured logging

Imposter can log a JSON summary of each request, such as the following:

```json
{
  "timestamp" : "2021-12-16T22:13:24.999Z",
  "uri" : "http://localhost:8080/pets/1",
  "path" : "/pets/1",
  "method" : "GET",
  "statusCode" : "200",
  "scriptTime" : "30.80",
  "duration" : "34.49"
}
```

To enable this, set the environment variable `IMPOSTER_LOG_SUMMARY=true`.

#### Logging request/response headers

You can optionally include request and response headers in the JSON summary such as:

```json
{
  "timestamp" : "2021-12-16T22:13:24.999Z",
  "uri" : "http://localhost:8080/pets/1",
  "path" : "/pets/1",
  "method" : "GET",
  "statusCode" : "200",
  "scriptTime" : "350.80",
  "duration" : "374.49",
  "x-correlation-id" : "aabbcc12345",
  "user-agent" : "Mozilla/5.0 (platform; rv:17.0) Gecko/geckotrail Firefox/90",
  "content-length" : "123"
}
```

> Note the presence of `x-correlation`, `user-agent` and `content-length` fields.

To add these, set the following environment variables:

    IMPOSTER_LOG_REQUEST_HEADERS="X-Correlation-ID,User-Agent"
    IMPOSTER_LOG_RESPONSE_HEADERS="Content-Length"

#### Logging request/response body

You can optionally include the request and/or response body in the JSON summary.

To enable these, set the following environment variables:

    IMPOSTER_LOG_REQUEST_BODY=true
    IMPOSTER_LOG_RESPONSE_BODY=true
