# Imposter: A scriptable, multipurpose mock server

Welcome to the Imposter documentation.

## Introduction

Imposter is a reliable, scriptable and extensible mock server for general REST APIs,
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications,
Salesforce and HBase APIs.

## What's it for?

You can use Imposter to:

* run standalone mocks in place of real systems
* decouple your integration tests from the cloud/various back-end systems and take control of your dependencies
* quickly set up a temporary API for your mobile/web client teams whilst the real API is being built

## Getting started

To begin, check out our [Getting started](getting_started.md) guide.

## User documentation

* [Getting started](getting_started.md)
* [Configuration guide](configuration.md)
* [Usage (command line arguments)](usage.md)
* [Tips and tricks](tips_tricks.md)

## Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks. You can load one or more plugins.

The following sections describe the built-in plugins. You can also write your own, if you want to customise behaviour further.

* [REST plugin](rest_plugin.md)
* [OpenAPI (aka Swagger) plugin](openapi_plugin.md)
* [HBase plugin](hbase_plugin.md)
* [SFDC (Salesforce) plugin](sfdc_plugin.md)

## Developers

* [Build](build.md)
* [Roadmap](roadmap.md)
