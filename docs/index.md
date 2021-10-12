# Imposter: A scriptable, multipurpose mock server

Welcome to the Imposter documentation.

## Introduction

Imposter is a reliable, scriptable and extensible mock server for REST APIs, OpenAPI (and Swagger) specifications, Salesforce and HBase APIs.

Run standalone mock servers, or embed mocks within your tests (supports JVM and Node.js). Dynamic responses can be scripted using JavaScript, Groovy or Java.

## Getting started

To begin, check out our [Getting started](getting_started.md) guide. See the _User documentation_ section below for more.

## Features

* run standalone mocks in place of real systems
* turn an OpenAPI/Swagger file into a mock API for dev or QA (use it before the real API is built)
* decouple your integration tests from the cloud/back-end systems and take control of your dependencies
* validate your API requests against an OpenAPI specification
* capture data and retrieve later, or use in templates to for conditional responses

Send dynamic responses:

- Provide mock responses using static files or customise behaviour based on characteristics of the request.
- Power users can control mock responses with JavaScript or Java/Groovy script engines.
- Advanced users can write their own plugins in a JVM language of their choice.

## User documentation

* [Getting started](getting_started.md)
* [Configuration guide](configuration.md)
* [Usage (arguments and environment variables)](usage.md)
* [Scripting](scripting.md)
* [Security](security.md)

### Data capture, storage and templates

* [Data capture](data_capture.md)
* [Response templates](templates.md)
* [Stores](stores.md)

### Advanced

* [Advanced request matching](request_matching.md)
* [OpenAPI validation](openapi_validation.md)
* [Performance simulation](performance_simulation.md)
* [Metrics, logs and telemetry](metrics_logs_telemetry.md)

### Other

* [Tips and tricks](tips_tricks.md)

## Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks. You can load one or more plugins.

> Learn more about [plugins and features](features_plugins.md).

The following sections describe the built-in plugins. You can also write your own, if you want to customise behaviour further.

* [OpenAPI (and Swagger) plugin](openapi_plugin.md)
* [REST plugin](rest_plugin.md)
* [HBase plugin](hbase_plugin.md)
* [SFDC (Salesforce) plugin](sfdc_plugin.md)

## Developers

* [Build](build.md)
* [Roadmap](roadmap.md)

## Tutorials

* [Mocking APIs with Swagger and Imposter](https://medium.com/@outofcoffee/mocking-apis-with-swagger-and-imposter-3694bd1733c0)
* [Mocking REST APIs with Imposter](https://medium.com/@outofcoffee/mocking-apis-with-imposter-53bd908632e5)
