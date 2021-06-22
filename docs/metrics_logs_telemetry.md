# Metrics, logs and telemetry

## Status endpoint

Imposter exposes a status endpoint under `/system/status`

This is useful as a healthcheck endpoint, such as for liveness or readiness checks.

```shell
$ curl http://localhost:8080/system/status

{
  "status":"ok"
  "version":"1.10.2-SNAPSHOT"
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

> Also see [the metrics example](./examples/metrics).

## Logs

Logs are printed to stdout.

You can control the logging level using the following environment variable:
    
    # also supports WARN, INFO, DEBUG etc.
    export IMPOSTER_LOG_LEVEL="TRACE"

Internally, Log4J2 is used, so the usual configuration options apply.
