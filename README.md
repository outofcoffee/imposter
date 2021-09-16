# Imposter: A scriptable, multipurpose mock server [![CircleCI](https://circleci.com/gh/outofcoffee/imposter/tree/master.svg?style=svg)](https://circleci.com/gh/outofcoffee/imposter/tree/master)

Reliable, scriptable and extensible mock server for REST APIs,
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (and Swagger) specifications,
Salesforce and HBase APIs.

Scripting support for both JavaScript or [Groovy/Java](http://www.groovy-lang.org/).

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

*****
## Getting started

The quickest way to get up and running is to use the free cloud-hosted service at **[mocks.cloud](https://www.mocks.cloud)**

## Tutorials

* [Mocking APIs with Swagger and Imposter](https://medium.com/@outofcoffee/mocking-apis-with-swagger-and-imposter-3694bd1733c0)
* [Mocking REST APIs with Imposter](https://medium.com/@outofcoffee/mocking-apis-with-imposter-53bd908632e5)

## User documentation

**[Read the user documentation here](https://outofcoffee.github.io/imposter/)**

*****

# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks:

* **[OpenAPI](docs/openapi_plugin.md)** - Support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (and Swagger) API specifications.
* **[REST](docs/rest_plugin.md)** - Mocks RESTful or plain HTTP APIs.
* **[HBase](docs/hbase_plugin.md)** - Basic HBase mock implementation.
* **[SFDC (Salesforce) plugin](docs/sfdc_plugin.md)** - Basic Salesforce mock implementation.

You can also create your own plugins, using a JVM language of your choice.

# Example

```shell
$ imposter up

Starting server on port 8080...
Parsing configuration file: someapi-config.yaml
...
Mock server is up and running
```

Your mock server is now running!

You can hit the URL [http://localhost:8080/users](http://localhost:8080/users) to see the mock response:

```shell
$ curl -v -X PUT http://localhost:8080/users/alice

HTTP/1.1 201 Created
Content-Type: application/json

{ "userName": "alice" }
```

# How to run Imposter

There are lots of way to run Imposter.

### Standalone mock server

1. Using the command line client - see [Imposter CLI](./docs/run_imposter_cli.md)
2. As a Docker container - see [Imposter Docker container](./docs/run_imposter_docker.md)
3. As a JAR file on the JVM - see [Imposter JAR file](./docs/run_imposter_jar.md)

### Embedded in tests

4. Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./distro/embedded/README.md)
5. Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

# Recent changes and Roadmap
  
For recent changes see the [Changelog](CHANGELOG.md), or view the [Roadmap](docs/roadmap.md).

# Contributing

* Pull requests are welcome.
* PRs should target the `develop` branch.

# Author

Pete Cornish (outofcoffee@gmail.com)
