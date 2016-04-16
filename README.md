# Imposter: A scriptable, multipurpose mock server [![Build Status](https://travis-ci.org/outofcoffee/imposter.svg?branch=master)](https://travis-ci.org/outofcoffee/imposter)

Decouple your integration tests from the cloud/various back-end systems. Imposter is a mock server with a suite of
plugins. It also allows you to write Groovy scripts to customise its behaviour.

## Plugins

Imposter supports different mock server types using plugins:

* rest - Simple REST API mock.
* sfdc - Basic Salesforce mock implementation.
* hbase - Basic HBase mock implementation.

## Example

     java -jar distro/build/libs/imposter.jar \
            -Dcom.gatehill.imposter.plugin=com.gatehill.imposter.plugin.rest.RestPluginImpl \
            -Dcom.gatehill.imposter.configDir=./plugin/rest/src/test/resources/config \
            -Dcom.gatehill.imposter.listenPort=8080

This starts a mock server using the simple REST plugin. Responses are served in line with the configuration files
inside the `config` folder. With the example above, you can hit the URL
[http://localhost:8080/example](http://localhost:8080/example) to see the mock response.

# Getting started

Note: See the _Usage_ section for the required arguments, and the examples below.

## Docker container

The easiest way to get started is to use the Docker container:

    docker run --ti -p 8443 outofcoffee/imposter [args]

## Java

You can build Imposter as a JAR file. See the _Build_ section below.

# Usage

The following system properties can be used (specify as command line switches with `-Dswitch=value`). 

    com.gatehill.imposter.plugin            Plugin class name
    com.gatehill.imposter.configDir         Directory containing mock configuration files
    com.gatehill.imposter.host              Host to which to bind when listening
    com.gatehill.imposter.listenPort        Port on which to listen
    com.gatehill.imposter.serverUrl         Explicitly set the server address, e.g. http://mypublicserver:8443
    com.gatehill.imposter.tls               Whether TLS/SSL is enabled
    com.gatehill.imposter.keyStorePath      Path to keystore
    com.gatehill.imposter.keyStorePassword  Keystore password

# Plugin examples

## rest

Simple REST API mock.

Example:

     java -jar distro/build/libs/imposter.jar \
            -Dcom.gatehill.imposter.plugin=com.gatehill.imposter.plugin.rest.RestPluginImpl \
            -Dcom.gatehill.imposter.configDir=./plugin/rest/src/test/resources/config

## sfdc

Basic Salesforce mock implementation. Supports non-persistent:

* SObject creation
* SObject update
* SObject retrieval by ID
* Dummy SOQL queries

**Note:** Clients interacting with this plugin usually requires TLS/SSL to be enabled. 
Ensure that you use an _https://_ scheme for accessing the mock server.

Example:

     java -jar distro/build/libs/imposter.jar \
            -Dcom.gatehill.imposter.plugin=com.gatehill.imposter.plugin.sfdc.SfdcPluginImpl \
            -Dcom.gatehill.imposter.configDir=./plugin/sfdc/src/test/resources/config \
            -Dcom.gatehill.imposter.tls=true \
            -Dcom.gatehill.imposter.keystorePath=./server/src/main/resources/keystore/ssl.jks \
            -Dcom.gatehill.imposter.keystorePassword=password

**Note:** This uses a self-signed certificate for TLS/SSL. You can also choose your own keystore.
If you need to trust the self-signed certificate when using the default, the keystore is located at
`server/src/main/resources/keystore` and uses the secure password 'password'.

## hbase

Basic HBase mock implementation. Uses protobuf for wire transport. Supports dummy Scanner queries.

Example:

     java -jar distro/build/libs/imposter.jar \
            -Dcom.gatehill.imposter.plugin=com.gatehill.imposter.plugin.hbase.HBasePluginImpl \
            -Dcom.gatehill.imposter.configDir=./plugin/hbase/src/test/resources/config

**Note:** This plugin will use the server URL in the `Location` header of the scanner creation response. You might
want to consider setting the `serverUrl` property explicitly to the publicly-accessible address of the mock server.

# Configuration

## Basics

Imposter configuration files must be named with a `-config.json` suffix. For example: `mydata-config.json`.

Response files can be named anything you like and are resolved relative to the configuration directory.

For example:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "basePath": "/example",
      "response": {
        "staticFile": "example-data.json"
      }
    }

Here's the corresponding static response file (`example-data.json`):

     {
       "hello": "world"
     }

You must specify the plugin to use in the configuration file. See the examples in this document for possible values.

## Advanced (scripting)

You can control Imposter's responses using [Groovy](http://groovy-lang.org/) scripts.

Here's an example configuration that uses a script file:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "basePath": "/scripted",
      "response": {
        "scriptFile": "example.groovy"
      }
    }

Here's the corresponding script (`example.groovy`):

    switch (context.params["action"]) {
        case "create":
            // HTTP Status-Code 201: Created.
            context.respondWithStatusCode 201
            break

        case "delete":
            // HTTP Status-Code 204: No Content.
            context.respondWithStatusCode 204
            break

        default:
            // default to bad request
            context.respondWithStatusCode 400
    }

In this example, the script causes the mock server to respond with HTTP status codes 201, 204 or 400 depending on
the value of the `action` parameter in the request.

For example:

    HTTP GET http://localhost:8443/scripted?action=201
    ...
    201 Created

    HTTP GET http://localhost:8443/scripted?action=foo
    ...
    400 Bad Request

## The InvocationContext

The `context` object in the example above is of type `com.gatehill.imposter.model.InvocationContext`.

As well as the `InvocationContext.respondWithStatusCode(int)` method, you can also take advantage of the
`InvocationContext.respondDefault()` method to respond with the content of a static file.
To do this, ensure you have set the `staticFile` property within the `response` object in your configuration.

For example:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "basePath": "/scripted",
      "response": {
        "scriptFile": "example.groovy",
        "staticFile": "example-data.json"
      },
      "contentType": "application/json"
    }

In this case, the static file `example-data.json` will be used if there is no script file specified, or if the script does not
invoke the `InvocationContext.respondWithStatusCode(int)` method, or if it explicitly invokes `InvocationContext.respondDefault()`.
See the *rest* plugin tests for a working example.

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

## Docker container

Build the Docker container with:

    docker build --tag outofcoffee/imposter .

# TODO

* Add script support to other plugins than just the _rest_ plugin
* HBase content negotiation
* HBase individual record retrieval
* HBase response content type header
* Config upload from tests (using a client library to wrap REST API?)

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)
