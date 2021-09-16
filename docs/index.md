# Imposter: A scriptable, multipurpose mock server

Welcome to the Imposter documentation.

## Introduction

Imposter is a reliable, scriptable and extensible mock server for REST APIs,
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (and Swagger) specifications,
Salesforce and HBase APIs.

## What's it for?

Run standalone mock servers, or embed mocks within your JVM or Node.js tests.

Use Imposter to:

* run standalone mocks in place of real systems
* turn an OpenAPI/Swagger file into a mock API for testing or QA
* quickly set up a temporary API for your mobile/web client teams whilst the real API is being built
* decouple your integration tests from the cloud/various back-end systems and take control of your dependencies
* validate your API requests against an OpenAPI specification
* capture data and use response templates to provide conditional responses

Send dynamic responses:

- Provide mock responses using static files or customise behaviour based on characteristics of the request.
- Power users can control mock responses with JavaScript or Java/Groovy script engines.
- Advanced users can write their own plugins in a JVM language of their choice.

## Getting started

To begin, check out our [Getting started](getting_started.md) guide.

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
