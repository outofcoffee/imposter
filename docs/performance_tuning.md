# Performance tuning

Imposter supports hundreds to thousands of requests per second, on a single CPU core and small memory footprint.

> See [Benchmarks](./benchmarks.md) for representative performance tests.

## Tuning

Some Imposter features have a greater impact than others on the performance of the mock engine.

### Turning off features

Switching off [features](./features_plugins.md) helps further reduce the memory and CPU requirements of the mock engine.

Features that can be switched off include:

- stores
- metrics collection

> See [features](./features_plugins.md) and [usage](./usage.md) documentation.

### Resource matching performance

[Resource matching](./configuration.md) is typically the fastest method of providing conditional responses. This is the case for request properties such as headers, query parameters, path parameters, path and HTTP method. In the case of using [JsonPath to query the request body](./request_matching.md) to conditionally match resources, however, the body must be parsed, which is computationally expensive and will result in lower performance. 

### Response Templating performance

[Templating](./templates.md) incurs a performance penalty, but is often faster than dynamically generating large objects using scripts, so is generally a better tradeoff when dynamic responses are required.

Template files are cached in memory once read from disk, so they do not incur as high an I/O cost from storage on subsequent requests.

Using JsonPath in placeholder templates is computationally expensive, as it requires parsing and querying of an item rather than just value substitution.

### Data Capture performance

[Data capture](./data_capture.md) incurs overhead on response times, depending on the speed of the store implementation used. If using the in-memory store, the performance impact is lower than using an external store. For store providers backed by external datastores, requests will incur a synchronous write to the store when capturing data.

You might consider using deferred capture, which has the advantage of improving request throughput, at the cost of persistence occurring after the request has been completed.

Using JsonPath to capture the request body is computationally expensive, as it requires parsing and querying of the request body item rather than just copying a reference.

## Benchmarks

See [Benchmarks](./benchmarks.md) for representative performance tests, including test set up and configuration.
