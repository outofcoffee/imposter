# Tips and tricks

This section provides more ideas on how to use Imposter as part of your project.

## Waiting for the server to be ready

The mock server exposes an endpoint at `/system/status` that will return HTTP 200 when the mock server is up and running. You can use this in your tests to know when the mock server is ready.

## Script logging

You can use the `logger` object within your scripts, which supports levels such as `info`, `debug` etc.

## Pinning a version

You can pin the version of Imposter so you always get the same engine version.

You can do this in a few ways using [the CLI](run_imposter_cli.md):

1. Pass the `--version x.y.z` argument to `imposter up`, or
2. Set `version: x.y.z` in an `.imposter.yaml` file in the config directory, or
3. Set `version: x.y.z` in your `$HOME/imposter/config.yaml` file

If you're using the Imposter [GitHub Actions](github_actions.md) in your CI/CD workflow, you can:

1. Set the `version` input to the `imposter-github-action/start-mocks` action, or
2. Set `version: x.y.z` in an `.imposter.yaml` file in the config directory

## Using a local Imposter JAR with the CLI

You can use a local Imposter JAR file with the CLI. 

1. Download the JAR file from the [releases page](https://github.com/outofcoffee/imposter/releases).
2. Set the `IMPOSTER_JVM_JARFILE` environment variable to the local path of the JAR file.
3. Start Imposter using the CLI, specifying the `jvm` engine type.

For example:

    export IMPOSTER_JVM_JARFILE=/path/to/imposter.jar
    imposter up -t jvm

## Standalone mocks

You can make use of Imposter mocks as standalone Docker containers.

See [bundling configuration](bundle.md#docker-bundles) for details on how to bundle your configuration and mock data into a Docker image.

## Testcontainers integration

You can make use of Imposter mocks in your [JUnit](http://junit.org) tests using the excellent [testcontainers](http://testcontainers.org) library. This will enable your mocks to start/stop before/after your tests run.

Here's a simple overview:

1. Follow the _testcontainers_ 'getting started' documentation for your project.
2. Add your mock configuration and mock data to your project (e.g. under `src/test/resources`).
3. Add a _testcontainers_ `GenericContainer` class rule to your JUnit test, for one of the Imposter Docker images (see _Docker_ section).
4. Configure your `GenericContainer` to mount the directory containing your configuration and data (e.g. `src/test/resources`) to `/opt/imposter/config`.
5. Configure your `GenericContainer` to wait for the `/system/status` HTTP endpoint to be accessible so your tests don't start before the mock is ready.

Now, when you run your test, your custom mock container will start, load your configuration and mock data, ready for your test methods to use it!

## Set OpenAPI spec base path

Set the environment variable `IMPOSTER_OPENAPI_SPEC_PATH_PREFIX`, to override the default `/_spec` prefix.

For example:

    IMPOSTER_OPENAPI_SPEC_PATH_PREFIX="/api/core-mock/v1/_spec"

...will make the spec UI accessible at http://localhost:8080/api/core-mock/v1/_spec/
