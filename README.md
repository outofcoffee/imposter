# Imposter: A multipurpose mock server [![Build Status](https://travis-ci.org/outofcoffee/imposter.svg?branch=master)](https://travis-ci.org/outofcoffee/imposter)

Example:

     java -jar distro/build/libs/imposter.jar \
            -Dcom.gatehill.imposter.plugin=com.gatehill.imposter.plugin.rest.RestPluginImpl \
            -Dcom.gatehill.imposter.configDir=./plugin/rest/src/test/resources/config

This starts a mock server using the simple REST plugin. Responses are served in line with the configuration files
inside the `config` folder. You can hit the URL [http://localhost:8443/example](http://localhost:8443/example) 
to see the mock response.

# Plugins

## rest

Simple REST API mock.

## hbase

Basic HBase mock implementation. Uses protobuf for wire transport. Supports dummy Scanner queries.

## sfdc

Basic Salesforce mock implementation. Supports non-persistent:

* SObject creation
* SObject update
* SObject retrieval by ID
* dummy SOQL queries

_Note:_ This plugin requires TLS/SSL to be enabled. Ensure you use an https:// scheme for accessing the mock server.

# Usage

The following system properties can be used (specify as command line switches with `-Dswitch=value`). 

    com.gatehill.imposter.plugin            Plugin class name
    com.gatehill.imposter.configDir         Directory containing mock configuration files
    com.gatehill.imposter.host              Host on which to listen
    com.gatehill.imposter.listenPort        Port on which to listen
    com.gatehill.imposter.tls               Whether TLS/SSL is enabled
    com.gatehill.imposter.keyStorePath      Path to keystore
    com.gatehill.imposter.keyStorePassword  Keystore password

# Build

## Prerequisites

* JDK 8

For distribution, Imposter is built as a 'fat JAR' (aka 'shadow JAR'). To do this yourself, run:

    ./gradlew clean shadowJar

If you want to compile the JAR without embedded dependencies, use:

    ./gradlew clean build

# Tests

If you want to run tests:

    ./gradlew clean test

# TODO

* Add Dockerfile

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)
