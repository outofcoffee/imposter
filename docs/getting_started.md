# Getting started

There are many ways to run Imposter. There are two categories: run a standalone server, or embed it within unit/integration tests.

### Standalone mock server

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

### Embed in unit/integration tests

- Embed within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embed within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

### Within your CI/CD pipeline

- Use the [Imposter GitHub Actions](./github_actions.md) to start and stop Imposter during your CI/CD pipeline.

---

## Choosing an approach

If you are looking for a quick local development solution, use [the CLI](./run_imposter_cli.md).

If you want to run Imposter as a standalone mock server, you can run it as a [Docker container](./run_imposter_docker.md). If Docker isn't your thing, or you want to [build](./build.md) Imposter yourself, you can use it as a standalone [JAR file](./run_imposter_jar.md). Standalone servers are often useful when you require long-lived mock instances.

You can also use it as a mock server for your unit tests on the [JVM](./embed_jvm.md) or [Node.js](https://github.com/imposter-project/imposter-js), starting it before your tests, providing synthetic responses to your unit under test. Embedded instances tend to be short-lived and exist only for the duration of your test execution.

### Examples

Let's assume your [configuration](./configuration.md) is in a folder named `config`.

CLI example:

    imposter up ./config -p 8080

Docker example:

    docker run -ti -p 8080:8080 -v $PWD/config:/opt/imposter/config outofcoffee/imposter

Standalone Java example:

    java -jar ./imposter.jar --configDir ./config

Your mock server is now running!

> These examples start a mock server using the simple [REST plugin](./rest_plugin.md), serving responses based on the configuration files inside the `config` folder. You can hit the URL [http://localhost:8080/example](http://localhost:8080/example) to see the mock response.

## What's next

Learn how to use Imposter with the [Configuration guide](configuration.md).
