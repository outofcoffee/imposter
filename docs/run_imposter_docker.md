# Running Imposter as a Docker container

There are many ways to run Imposter. This section describes how to use the Docker container.

> ### Other ways to run Imposter
> 
> #### Standalone mock server
> 
> - Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
> - As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
> - As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)
> 
> #### Embedded in tests
> 
> - Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md) 
> - Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

## Docker Image Features

- Start mocks
- Supports all [plugins](./plugins.md)

## Prerequisites

You must have [Docker](https://docs.docker.com/get-docker/) installed.

The easiest way to get started is to use an Imposter Docker container, such as:

    docker run -ti -p 8080:8080 outofcoffee/imposter-rest [args]

## Docker images

Most users should choose the 'core' image available at: [outofcoffee/imposter](https://hub.docker.com/r/outofcoffee/imposter). This is the primary Imposter Docker image supporting both OpenAPI and plain REST APIs.

The following images are available:

| Image       | Docker Hub link                                                                       | Plugins             | Notes                                                                                                                             |
|-------------|---------------------------------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **core**    | [outofcoffee/imposter](https://hub.docker.com/r/outofcoffee/imposter)                 | openapi, rest, soap | This is the primary Imposter Docker image supporting OpenAPI, plain REST APIs and SOAP/WSDL mocks. Most users should choose this. |
| **openapi** | [outofcoffee/imposter-openapi](https://hub.docker.com/r/outofcoffee/imposter-openapi) | openapi             | Only contains the [OpenAPI mock plugin](./openapi_plugin.md).                                                                     |
| **rest**    | [outofcoffee/imposter-rest](https://hub.docker.com/r/outofcoffee/imposter-rest)       | rest                | Only contains the [REST mock plugin](./rest_plugin.md).                                                                           |
| **all**     | [outofcoffee/imposter-all](https://hub.docker.com/r/outofcoffee/imposter-all)         | All plugins         | Contains all mock plugins, and is the largest and has the most dependencies.                                                      |

> You can also use the these images to create your own custom images with embedded configuration.

## Configuration location

When running the Imposter Docker container, place your files at the path within the container:

    /opt/imposter/config

This is the location that the mock engine looks for configuration files (i.e. those with the `-config.yaml` suffix). Any files referenced from your configuration files will be resolved relative to this path within the container.

---

## Run container

To run Imposter, map the path to the configuration files directory to the `/opt/imposter/config` directory within the container.

    docker run -ti -v /path/to/config:/opt/imposter/config -p 8080:8080 outofcoffee/imposter

> To get up and running quickly, see the [examples](https://github.com/outofcoffee/imposter/tree/main/examples).

### Example

```shell
$ docker run -it -p 8080:8080 -v $PWD/example-api:/opt/imposter/config outofcoffee/imposter

Starting mock engine 2.13.1
Loading configuration file: /opt/imposter/config/someapi-config.yaml
...
Mock engine up and running on http://localhost:8080
```

The mock server is running at [http://localhost:8080](http://localhost:8080)

---

## Build a self-contained image

You can also build a self-contained image, containing both the Imposter mock engine and your configuration files. This makes your mock portable to wherever Docker runs.

Let's assume the following file structure:

```
.
├── Dockerfile
└── config
    ├── petstore-config.yaml
    └── petstore.yaml
```

The content of the `Dockerfile` would be as follows:

```
FROM outofcoffee/imposter
COPY ./config/* /opt/imposter/config/
```

Build your container image as follows:

```shell
$ docker build -t imposter-example .

Sending build context to Docker daemon   5.12kB
Step 1/2 : FROM outofcoffee/imposter
 ---> 36d19405d09b
Step 2/2 : COPY ./config/* /opt/imposter/config/
 ---> 1f2667a1d5e5
Successfully built 1f2667a1d5e5
Successfully tagged imposter-example:latest
```

The container image `imposter-example` contains both the Imposter mock engine and your configuration files from the `config` directory.

Run the container:

```shell
$ docker run -it -p 8080:8080 imposter-example

Starting mock engine 2.13.1
Loading configuration file: /opt/imposter/config/petstore-config.yaml
...
Mock engine up and running on http://localhost:8080
```

The mock server is running at [http://localhost:8080](http://localhost:8080)

## Custom Docker images

The default container images are stripped back and do not have any binaries under `/usr/bin`. This is to reduce their size and attack surface.

One consequence is that shell commands in custom Dockerfiles don't work with the default Docker images when simply extending `FROM` them as the base image.

However, it is possible to build container images and use shell commands in a Dockerfile, if you build a container image using [multi-stage builds](https://docs.docker.com/build/building/multi-stage/).

> **An example**
> 
> Here is an example of a custom Docker image: https://github.com/outofcoffee/imposter/blob/main/examples/docker/Dockerfile
>
> This example extends from the Java base image so has its various userspace tools available (and more you can install with the package manager).

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
- See full [usage instructions](./usage.md).
