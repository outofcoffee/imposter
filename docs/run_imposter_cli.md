# Running Imposter with the CLI

There are many ways to run Imposter. This section describes using the command line interface (CLI) tool.

<details>
<summary>Other ways to run Imposter</summary>

#### Standalone mock server

- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

#### Embedded in tests

- Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

### Within your CI/CD pipeline

- Use the [Imposter GitHub Actions](./github_actions.md) to start and stop Imposter during your CI/CD pipeline.

</details>

## CLI Features

- Start mocks (`imposter up`)
- Generate mock configuration from OpenAPI files (`imposter scaffold`)
- Supports all [plugins](./plugins.md)
- Supports JVM and Docker engine types
- Supports both 'core' and 'all' distributions

## Installation

### Prerequisites

You must have [Docker](https://docs.docker.com/get-docker/) or a JVM installed.

### Homebrew

If you have Homebrew installed:

    brew tap gatehill/imposter
    brew install imposter

### Shell script

Or, use this one liner (macOS and Linux only):

```shell
curl -L https://raw.githubusercontent.com/imposter-project/imposter-cli/main/install/install_imposter.sh | bash -
```

### Other installation options

See the full [Installation](https://github.com/imposter-project/imposter-cli/blob/main/docs/install.md) instructions for your system.

## Example

```shell
$ cd /path/to/config
$ imposter up

Starting server on port 8080...
Parsing configuration file: someapi-config.yaml
...
Mock server is up and running
```

## Different distributions

The previous command starts Imposter using the 'core' distribution, which includes common [plugins](./plugins.md) only. To use the 'all' distribution, which includes all plugins, use the `-t` (engine type) flag:

```shell
$ imposter up -t docker-all
```

## CLI usage

See full usage instructions on [Imposter CLI](https://github.com/imposter-project/imposter-cli).

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
