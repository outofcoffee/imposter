# Getting started

There are lots of way to run Imposter.

### Standalone mock server

1. Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
2. As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
3. As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

### Embedded in tests

4. Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](../distro/embedded/README.md)
5. Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

---

## Choosing an approach

If you are looking for a quick local development solution, use [the CLI](./run_imposter_cli.md).

If you want to run Imposter as a standalone mock server, you can run it as a [Docker container](./run_imposter_docker.md). If Docker isn't your thing, or you want to [build](./build.md) Imposter yourself, you can use it as a standalone [JAR file](./run_imposter_jar.md).

You can also use it as a mock server for your unit tests on the [JVM](../distro/embedded/README.md) or [Node.js](https://github.com/gatehill/imposter-js), starting it before your tests, providing synthetic responses to your unit under test.

### Examples

Let's assume your [configuration](./configuration.md) is in a folder named `config`.

CLI example:

    imposter up ./config -p 8080

Docker example:

    docker run -ti -p 8080:8080 -v $PWD/config:/opt/imposter/config outofcoffee/imposter-rest

Standalone Java example:

    java -jar ./imposter.jar --configDir ./config

Your mock server is now running!

> These examples start a mock server using the simple [REST plugin](./rest_plugin.md), serving responses based on the configuration files inside the `config` folder. You can hit the URL [http://localhost:8080/example](http://localhost:8080/example) to see the mock response.

## What's next

Learn how to use Imposter with the [Configuration guide](configuration.md).
