# Running Imposter as a Docker container

There are many ways to run Imposter. This section describes how to use the Docker container.

---
### Other ways to run Imposter

#### Standalone mock server

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

#### Embedded in tests

- Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](../distro/embedded/README.md) 
- Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

---

## Docker Image Features

- Start mocks
- Supports all [plugins](./features_plugins.md)

## Run

### Prerequisites

You must have [Docker](https://docs.docker.com/get-docker/) installed.

The easiest way to get started is to use an Imposter Docker container, such as:

    docker run -ti -p 8080:8080 outofcoffee/imposter-rest [args]

### Docker images

The following images are available:

| Image           | Docker Hub link                                                                       | Notes                |
|-----------------|---------------------------------------------------------------------------------------|----------------------|
| **openapi**     | [outofcoffee/imposter-openapi](https://hub.docker.com/r/outofcoffee/imposter-openapi) |                      |
| **rest**        | [outofcoffee/imposter-rest](https://hub.docker.com/r/outofcoffee/imposter-rest)       |                      |
| **hbase**       | [outofcoffee/imposter-hbase](https://hub.docker.com/r/outofcoffee/imposter-hbase)     |                      |
| **sfdc**        | [outofcoffee/imposter-sfdc](https://hub.docker.com/r/outofcoffee/imposter-sfdc)       |                      |
| **all**         | [outofcoffee/imposter](https://hub.docker.com/r/outofcoffee/imposter)                 | Supports all plugins |

> You can also use the these images to create your own custom images with embedded configuration.

### Run container

Run using Docker:

    docker run -ti -p 8080:8080 \
        -v /path/to/config:/opt/imposter/config \
        outofcoffee/imposter-rest [args]

...ensuring that you choose the right image for the [plugin](./features_plugins.md) you wish to use.

## Example

```shell
$ docker run --rm -it -p 8080:8080 -v $PWD/example-api:/opt/imposter/config outofcoffee/imposter-rest

Starting server on port 8080...
Parsing configuration file: someapi-config.yaml
...
Mock server is up and running
```

## Usage

See full [usage instructions](./usage.md).

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
