# OpenAPI (aka Swagger) plugin

Plugin class: `com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl`

The plugin provides support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications.

## Features

* Creates mock endpoints from OpenAPI/Swagger 2 API specifications.
* Serves response examples embedded in the specification.
* Also supports static response files and script-driven responses, using status code, response files etc.
* Provides an interactive API sandbox at `/_spec`

# Configuration

Read the [Configuration](configuration.md) section to understand how to configure Imposter.

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `operation` | `io.swagger.models.Operation` | The OpenAPI operation for the request.

## Using the plugin

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

![API sandbox](images/api-sandbox.png)

## Example

For working examples, see:

    plugin/openapi/src/test/resources/config
