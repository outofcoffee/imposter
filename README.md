# Imposter: A scriptable, multipurpose mock server [![Build Status](https://travis-ci.org/outofcoffee/imposter.svg?branch=master)](https://travis-ci.org/outofcoffee/imposter)

Reliable, scriptable and extensible mock server for general REST APIs,
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications,
Salesforce and HBase APIs.

Scripting support for both [JavaScript](https://www.javascript.com/) or [Groovy/Java](http://www.groovy-lang.org/).

## What's it for?

Use Imposter to:

* run standalone mocks in place of real systems
* turn a Swagger file into a mock API for testing or QA
* quickly set up a temporary API for your mobile/web client teams whilst the real API is being built
* decouple your integration tests from the cloud/various back-end systems and take control of your dependencies

Provide mock responses using static files or customise behaviour using JavaScript or Java/Groovy. Power users can write their own plugins in a JVM language of their choice.

*****
# Documentation

**[Read the documentation here.](http://outofcoffee.viewdocs.io/imposter/)**
*****

# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks:
This section describes the available plugins. You can also write your own, if you want to further customise behaviour.

## REST plugin

Plugin class: `com.gatehill.imposter.plugin.rest.RestPluginImpl`

### Features

* Simple REST API mock.
* Supports arbitrary format static files.
* Supports optional JSON array responses.

### Additional context objects

None.

### Example

For working examples, see:

    plugin/rest/src/test/resources/config

## OpenAPI (aka Swagger) plugin

Plugin class: `com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl`

The plugin provides support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications.

### Features

* Creates mock endpoints from OpenAPI/Swagger 2 API specifications.
* Serves response examples embedded in the specification.
* Also supports static response files and script-driven responses, using status code, response files etc.
* Provides an interactive API sandbox at `/_spec`

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `operation` | `io.swagger.models.Operation` | The OpenAPI operation for the request.

### Example

For working examples, see:

    plugin/openapi/src/test/resources/config

A great way to use this plugin is to take advantage of the built in `examples` feature of OpenAPI/Swagger files.
These provide a standard way to document sample responses for each API response. This plugin will
match the example to serve using a combination of:

* matching URI/path
* matching content type in `Accept` HTTP request header to the `produces` property of the response
* matching status code to the response

Typically you will use a simple script (see `plugin/openapi/src/test/resources/config` for working example)
to control the status code, and thus the content of the response.

You can also use the interactive API sandbox at `/_spec`; e.g. [http://localhost:8443/_spec](http://localhost:8443/_spec),
which looks like this:

![API sandbox](docs/images/api-sandbox.png)

## Salesforce (SFDC) plugin

Plugin class: `com.gatehill.imposter.plugin.sfdc.SfdcPluginImpl`

### Features

* Basic Salesforce mock implementation.
* Non-persistent SObject creation.
* Non-persistent SObject update.
* SObject retrieval by ID.
* Dummy SOQL queries.

**Note:** Clients interacting with this plugin usually requires TLS/SSL to be enabled. 
Ensure that you use an _https://_ scheme for accessing the mock server. See the TLS/SSL section in this document
for more details.

### Additional context objects

None.

### Example

For working examples, see:

    plugin/sfdc/src/test/resources/config

## HBase plugin

Plugin class: `com.gatehill.imposter.plugin.hbase.HBasePluginImpl`

### Features

* Basic HBase mock implementation.
* Supports protobuf or JSON for wire transport.
* Dummy Scanner queries.
* Individual table row/record retrieval.

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `tableName` | `String` | The name of the HBase table.
| `responsePhase` | `com.gatehill.imposter.plugin.hbase.model.ResponsePhase` | The type of response being served.
| `scannerFilterPrefix` | `String` | The prefix from the filter of the result scanner.
| `recordInfo` | `RecordInfo` | The record info contains recordId.

### Example

For working examples, see:

    plugin/hbase/src/test/resources/config

**Note:** This plugin will use the server URL in the `Location` header of the scanner creation response. You might
want to consider setting the `serverUrl` property explicitly to the publicly-accessible address of the mock server.

# Configuration

## Basics

* **[REST](docs/rest_plugin.md)** - Mocks RESTful or plain HTTP APIs.
* **[OpenAPI (aka Swagger)](docs/openapi_plugin.md)** - Support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) API specifications.
* **[HBase](docs/hbase_plugin.md)** - Basic HBase mock implementation.
* **[SFDC (Salesforce) plugin](docs/sfdc_plugin.md)** - Basic Salesforce mock implementation.

You can also create your own plugins, using a JVM language of your choice.

# Example

Let's assume your [configuration](docs/configuration.md) is in a folder named `config`.

Docker example:

    docker run -ti -p 8443:8443 \
        -v $(pwd)/config:/opt/imposter/config \
        outofcoffee/imposter-rest

Standalone Java example:

    java -jar distro/build/libs/imposter.jar \
        --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
        --configDir ./config

Your mock server is now running!

This example starts a mock server using the simple
[REST plugin](docs/rest_plugin.md), serving responses based on the configuration files
inside the `config` folder. You can hit the URL
[http://localhost:8443/example](http://localhost:8443/example) to see the mock response.

# Recent changes and Roadmap
  
For recent changes see the [Changelog](CHANGELOG.md), or view the [Roadmap](docs/roadmap.md).

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)
