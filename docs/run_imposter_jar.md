# Running Imposter as a JAR on the JVM

There are lots of way to run Imposter.

### Standalone mock server

1. Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
2. As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
3. As a JAR file on the JVM - see below

### Embedded in tests

4. Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](../distro/embedded/README.md) 
5. Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js) 

---

## JAR File Features

- Start mocks
- Supports all [plugins](./features_plugins.md)

> Note: if you want to embed Imposter in your JUnit tests, see [JVM bindings](../distro/embedded/README.md) instead.

## Run

### Prerequisites

You must have a JVM installed. Java 8, 11 and 15 have been confirmed as compatible. Others may also be, but are not currently tested.

Run the JAR as follows:

    java -jar distro/all/build/libs/imposter-all.jar \
        --plugin <plugin name> \
        --configDir <config dir> \
        [args]

...ensuring that you choose the right [plugin](./features_plugins.md) you wish to use, for example:

    java -jar distro/all/build/libs/imposter-all.jar \
        --plugin rest \
        --configDir /path/to/config \
        [args]

## Example

```shell
$ java -jar ./imposter-all.jar --plugin rest --configDir ./example-api

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
