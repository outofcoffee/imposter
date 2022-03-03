---
layout: default
title: Documentation
description: Imposter Mock Engine
---

## Welcome

Imposter is a mock server for REST APIs, OpenAPI (and Swagger) specifications, SOAP web services (and WSDL files), Salesforce and HBase APIs.

- Run **standalone** mock servers in Docker, Kubernetes, AWS Lambda or on the JVM.
- **Embed** mocks within your tests (JVM or Node.js) to remove external dependencies.
- Script **dynamic** responses using JavaScript, Groovy or Java.
- **Capture** data from requests, then store it or return a **templated** response.

## Getting started

To begin, check out the [Getting started](getting_started.md) guide. See the _User documentation_ section below, or read a [product summary](./summary.md).

## User documentation

- [Configuration guide](configuration.md)
- [Usage (arguments and environment variables)](usage.md)
- [Scripting](scripting.md)
- [Security](security.md)

### Data capture, storage and templates

- [Data capture](data_capture.md)
- [Response templates](templates.md)
- [Stores](stores.md)

### Advanced

- [Advanced request matching](request_matching.md)
- [OpenAPI validation](openapi_validation.md)
- [Performance simulation](performance_simulation.md)
- [Metrics, logs and telemetry](metrics_logs_telemetry.md)
- [Performance tuning](./performance_tuning.md)
- [Plugins](./plugins.md)
- [Features](./features.md)

### Other

- [Tips and tricks](tips_tricks.md)
- [Benchmarks](./benchmarks.md)

## Mock types

Imposter provides specialised mocks for the following scenarios:

- **[OpenAPI](openapi_plugin.md)** - Support for OpenAPI (and Swagger) API specifications.
- **[REST](rest_plugin.md)** - Mocks RESTful or plain HTTP APIs.
- **[SOAP](soap_plugin.md)** - Support for SOAP web services (and WSDL files).
- **[HBase](hbase_plugin.md)** - Basic HBase mock implementation.
- **[SFDC (Salesforce)](sfdc_plugin.md)** - Basic Salesforce mock implementation.

> Learn more about [plugins](plugins.md).

## Developers

- [Build](build.md)
- [Roadmap](roadmap.md)
- [Github](https://github.com/outofcoffee/imposter)

## Tutorials

- [Mocking APIs with Swagger and Imposter](https://medium.com/@outofcoffee/mocking-apis-with-swagger-and-imposter-3694bd1733c0)
- [Mocking REST APIs with Imposter](https://medium.com/@outofcoffee/mocking-apis-with-imposter-53bd908632e5)
- [Mocking SOAP web services with Imposter](https://medium.com/@outofcoffee/mocking-soap-web-services-with-imposter-da8e9666b5b4)
