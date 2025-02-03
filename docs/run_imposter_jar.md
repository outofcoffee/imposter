# Running Imposter as a JAR on the JVM

There are many ways to run Imposter. This section describes how to use a JAR file on the JVM.

<details>
<summary>Other ways to run Imposter</summary>

### Standalone mock server

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)

### Embed in unit/integration tests

- Embed within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embed within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

### Within your CI/CD pipeline

- Use the [Imposter GitHub Actions](./github_actions.md) to start and stop Imposter during your CI/CD pipeline.

</details>

## JAR File Features

- Start mocks
- Supports all [plugins](./plugins.md)

> Note: if you want to embed Imposter in your JUnit tests, see [JVM bindings](./embed_jvm.md) instead.

## Run

### Prerequisites

You must have a JVM installed. Java 11 and 15 have been confirmed as compatible. Others may also be, but are not currently tested.

> Note: As of version 3, Imposter no longer supports Java 8. If Java 8 support is required, Imposter 2.x is still available, but may no longer be maintained.

Run the JAR as follows:

    java -jar distro/all/build/libs/imposter-all.jar \
        --plugin <plugin name> \
        --configDir <config dir> \
        [args]

...ensuring that you choose the right [plugin](./plugins.md) you wish to use, for example:

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
