# Imposter: A scriptable, multipurpose mock server [![Build Status](https://travis-ci.org/outofcoffee/imposter.svg?branch=master)](https://travis-ci.org/outofcoffee/imposter)

Mock server for general REST, Salesforce and HBase APIs. Also supports
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) APIs.

Imposter is a mock server with a suite of plugins. Use it to decouple your integration tests from the cloud/various back-end systems or run standalone mocks for your application.

Response behaviour can use static files, or customised using Groovy scripts. Power users can write thier own plugins in a JVM language of their choice.

## Which plugins are available?

Imposter supports different mock server types using plugins:

* **rest** - Mocks RESTful APIs.
* **openapi** - Support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) API specifications.
* **sfdc** - Basic Salesforce mock implementation.
* **hbase** - Basic HBase mock implementation.

## Example

Docker example:

    docker run -ti -p 8443:8443 \
            -v $(pwd)/plugin/rest/src/test/resources/config:/opt/imposter/config \
            outofcoffee/imposter-rest

Standalone Java example:

    java -jar distro/build/libs/imposter.jar \
            --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
            --configDir ./plugin/rest/src/test/resources/config

This starts a mock server using the simple REST plugin. Responses are served based on the configuration files
inside the `config` folder. With the example above, you can hit the URL
[http://localhost:8443/example](http://localhost:8443/example) to see the mock response.

# Getting started

Note: See the _Usage_ section for the required arguments, and the examples below.

## Running as a Docker container

The easiest way to get started is to use an Imposter Docker container, such as:

    docker run -ti -p 8443:8443 outofcoffee/imposter-rest [args]

### Docker images

The following images are available:

* `outofcoffee/imposter-rest:latest`
* `outofcoffee/imposter-openapi:latest`
* `outofcoffee/imposter-hbase:latest`
* `outofcoffee/imposter-sfdc:latest`

_Note:_ There is also a base container that does not enable any plugins:

    outofcoffee/imposter:latest

You can use the base image to create your own custom images.

### Example

If you want to run Imposter using Docker, use:

    docker run -ti -p 8443:8443 \
            -v /path/to/config:/opt/imposter/config \
            outofcoffee/imposter-rest [args]

...ensuring that you choose the right image for the plugin you wish to use.

## Running as a standalone Java application

If Docker isn't your thing, or you want to build Imposter yourself, you can create a standlone JAR file. See the _Build_ section below.

Once, built, you can run the JAR as follows:

    java -jar distro/build/libs/imposter.jar \
            --plugin <plugin class> \
            --configDir <config dir> \
            [args]

...ensuring that you choose the right plugin class for the plugin you want to use, for example:

    java -jar distro/build/libs/imposter.jar \
            --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
            --configDir /path/to/config \
            [args]

# Usage

The following command line arguments can be used:

     --configDir (-c) VAL   : Directory containing mock configuration files
     --help (-h)            : Display usage only
     --host (-b) VAL        : Bind host
     --keystorePassword VAL : Password for the keystore (default: password)
     --keystorePath VAL     : Path to the keystore (default: classpath:/keystore/ssl.jks)
     --listenPort (-l) N    : Listen port (default: 8443)
     --plugin (-p) VAL      : Plugin class name
     --serverUrl (-u) VAL   : Explicitly set the server address
     --tlsEnabled (-t)      : Whether TLS (HTTPS) is enabled (requires keystore to be configured) (default: false)
     --version (-v)         : Print version and exit

# Plugins

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

* OpenAPI/Swagger 2 API specifications
* Response examples inside the specification
* Static response files
* Script-driven responses, using status code, response files etc.

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
* Uses protobuf for wire transport.
* Dummy Scanner queries.
* Individual table row/record retrieval.

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `tableName` | `String` | The name of the HBase table.
| `responsePhase` | `com.gatehill.imposter.plugin.hbase.model.ResponsePhase` | The type of response being served.
| `scannerFilterPrefix` | `String` | The prefix from the filter of the result scanner.

### Example

For working examples, see:

    plugin/hbase/src/test/resources/config

**Note:** This plugin will use the server URL in the `Location` header of the scanner creation response. You might
want to consider setting the `serverUrl` property explicitly to the publicly-accessible address of the mock server.

# Configuration

## Basics

Imposter configuration files must be named with a `-config.json` suffix. For example: `mydata-config.json`.

Here is an example configuration file:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/example",
      "response": {
        "staticFile": "example-data.json"
      }
    }

**Note:** You must specify the plugin to use in the configuration file. See the examples in this document for possible values.

## Simple (static response files)

You can control Imposter's responses using static response files. Use the `staticFile` property
within the `response` object in your configuration.

Response files can be named anything you like and their path is resolved relative to the server's configuration directory.

In the example above, we are using a static response file (`example-data.json`):

     {
       "hello": "world"
     }

## Advanced (scripting)

You can also control Imposter's responses using [Groovy](http://groovy-lang.org/) scripts. Since it's Groovy, you
can write plain Java in your scripts as well.

Here's an example configuration file that uses a script:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/scripted",
      "response": {
        "scriptFile": "example.groovy"
      }
    }

Here's the corresponding script (`example.groovy`):

    if (context.request.params["action"] == "create") {
        // HTTP Status-Code 201: Created.
        respond {
            withStatusCode 201
            immediately()
        }
    }

In the example above, the script causes the mock server to respond with HTTP status code 201 if the value of
the `action` parameter in the request is `create`.

Here's a more sophisticated example:

    switch (context.request.params["action"]) {
        case "create":
            // HTTP Status-Code 201: Created.
            respond {
                withStatusCode 201
                immediately()
            }
            break

        case "fetch":
            // use a different static response file with the default behaviour
            respond {
                withFile "static-data.json"
                and()
                usingDefaultBehaviour()
            }
            break

        default:
            // default to bad request
            respond {
                withStatusCode 400
                immediately()
            }
            break
    }

In this example, the script causes the mock server to respond with HTTP status codes 200, 201 or 400 depending on
the value of the `action` parameter in the request.

For example:

    HTTP GET http://localhost:8443/scripted?action=201
    ...
    201 Created

    HTTP GET http://localhost:8443/scripted?action=foo
    ...
    400 Bad Request

In the case of `action=fetch`, the script causes the mock server to use the content of the static file
`static-data.json` to serve the response.

## Script objects

Certain objects are available to your scripts.

| Object | Description
| --- | ---
| `context` | Convenience object for accessing request properties
| `logger` | Logger, supporting levels such as `info(String)`, `warn(String)` etc.

## The context object

The `context` object is available to your scripts. It holds things you might like to interrogate,
like the request object.

| Property | Description
| --- | ---
| `request` | The HTTP request.

**Note:** Certain plugins will add additional properties to the `context`. For example, the _hbase_
plugin provides a `tableName` object, which you can use to determine the HBase table for the request being served.

## The request object

The request object is available on the `context`. It provides access to request parameters, method, URI etc.

| Property | Description | Example                                                                                                                                                           
| --- | --- | --------------------------------------
| `method` | The HTTP method of the request. | `"GET"`                                                                                                                                                           
| `uri` | The absolute URI of the request. | `"http://example.com?foo=bar&baz=qux"`
| `params` | A `Map` containing the request parameters. | `[ "foo": "bar", "baz": "qux" ]`                              
| `body` | A `String` containing the request body. | `"Hello world."`                                                                                                                    

## The ResponseBehaviour object

Your scripts are a subclass of `com.gatehill.imposter.model.ResponseBehaviour`.

The ResponseBehaviour class provides a number of methods to enable you to control the mock server response:
 
| Method | Description
| --- | ---
| `withStatusCode(int)` | Set the HTTP status code for the response
| `withFile(String)` | Respond with the content of a static file
| `withEmpty()` | Respond with empty content, or no records
| `usingDefaultBehaviour()` | Use the plugin's default behaviour to respond
| `immediately()` | Skip the plugin's default behaviour and respond immediately
| `and()` | Syntactic sugar to improve readability of `respond` statements

Typically you structure your response behaviours like so:

    respond {
        // behaviours go here
    }
    
For example:

    respond {
        withStatusCode 201
        and()
        usingDefaultBehaviour()
    }

## Returning data

To return data when using a script, you specify a response file. To specify which response file to use, you can either:

1. set the `staticFile` property within the `response` object in your configuration, or
2. call the `ResponseBehaviour.withFile(String)` in your script.

The response file is used by the active plugin to generate a response. For example, the _rest_ plugin might return
the content of the file unmodified, however, the _hbase_ and _sfdc_ plugins use the response file to generate
responses that mimic their respective systems.

Here's an example of the static file approach:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/scripted",
      "response": {
        "scriptFile": "example.groovy",
        "staticFile": "example-data.json"
      },
      "contentType": "application/json"
    }

Here, the response file `example-data.json` will be used, unless the script invokes the 
`ResponseBehaviour.withFile(String)` method with a different filename.

In order for the mock server to return the response file in an appropriate format, the plugin must be allowed to
process it. That means you should not call `ResponseBehaviour.immediately()` unless you want to skip using a response file (e.g. if
you want to send an error code back or a response without a body).

Whilst not required, your script could invoke `ResponseBehaviour.usingDefaultBehaviour()` for readability to indicate
that you want the plugin to handle the response file for you. See the *rest* plugin tests for a working example. To this
end, the following blocks are semantically identical:

    respond {
        withFile "static-data.json" and() usingDefaultBehaviour()
    }

and:

    respond {
        withFile "static-data.json"
    }

## TLS/SSL

You can run Imposter with HTTPS enabled. To do this, enable the TLS option and provide keystore options.

### Example

    java -jar distro/build/libs/imposter.jar \
            --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
            --configDir /path/to/config \
            --tlsEnabled \
            --keystorePath ./server/src/main/resources/keystore/ssl.jks \
            --keystorePassword password

**Note:** This uses a self-signed certificate for TLS/SSL. You can also choose your own keystore.
If you need to trust the self-signed certificate when using the default, the keystore is located at
`server/src/main/resources/keystore` and uses the secure password 'password'.

# Tips and tricks

## Waiting for the server to be ready

The mock server exposes an endpoint at `/system/status` that will return HTTP 200 when the mock server is up and running.
You can use this in your tests to know when the mock server is ready.

## Script logging

You can use the `logger` object within your scripts, which supports levels such as `info`, `debug` etc.

## Standalone mocks

You can make use of Imposter mocks as standalone Docker containers.

Here's a simple overview:

1. Create a simple _Dockerfile_ that extends `outofcoffee/imposter` and adds your desired properties as its `CMD`.
2. Add your mock configuration and mock data to `/opt/imposter/config` within the Docker image.
3. Build an image from your _Dockerfile_.

Now, when you start a container from your image, your standalone mock container will start, load your configuration and
mock data, and listen for connections.

## JUnit integration

You can make use of Imposter mocks in your [JUnit](http://junit.org) tests using the excellent
[testcontainers](http://testcontainers.org) library. This will enable your mocks to start/stop before/after your
tests run.

Here's a simple overview:

1. Follow the _testcontainers_ 'getting started' documentation for your project.
2. Add your mock configuration and mock data to your project (e.g. under `src/test/resources`).
3. Add a _testcontainers_ `GenericContainer` class rule to your JUnit test, for one of the Imposter Docker images (see _Docker_ section).
4. Configure your `GenericContainer` to mount the directory containing your configuration and data (e.g. `src/test/resources`) to `/opt/imposter/config`.
5. Configure your `GenericContainer` to wait for the `/system/status` HTTP endpoint to be accessible so your tests don't start before the mock is ready.

Now, when you run your test, your custom mock container will start, load your configuration and mock data, ready
for your test methods to use it!

# Build

## Prerequisites

* JDK 8

## Steps

For distribution, Imposter is built as a 'fat JAR' (aka 'shadow JAR'). To get started with the examples here, first run:

    ./gradlew clean shadowJar
    
The JAR is created under the `distro/build/libs` directory.

If, instead, you want to compile the JAR without embedded dependencies, use:

    ./gradlew clean build

## Tests

If you want to run tests:

    ./gradlew clean test

## Docker containers

Build the Docker containers with:

    cd docker
    ./build.sh

# TODO

* HBase response content type header
* Reuse HBase model classes for JSON serialisation
* Execute mock processing asynchronously
* Docker images shadow JARs should only depend on a single plugin module

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)