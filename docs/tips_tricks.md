# Tips and tricks

This section provides more ideas on how to use Imposter as part of your project.

## Waiting for the server to be ready

The mock server exposes an endpoint at `/system/status` that will return HTTP 200 when the mock server is up and running.
You can use this in your tests to know when the mock server is ready.

## Script logging

You can use the `logger` object within your scripts, which supports levels such as `info`, `debug` etc.

## Standalone mocks

You can make use of Imposter mocks as standalone Docker containers.

Here's a simple overview:

1. Create a simple _Dockerfile_ that extends `outofcoffee/imposter` and adds your desired properties as its `CMD`.
2. Add your mock configuration and mock data to `/opt/imposter/config` within the Docker image.
3. Build an image from your _Dockerfile_.

Now, when you start a container from your image, your standalone mock container will start, load your configuration and
mock data, and listen for connections.

## JUnit integration

You can make use of Imposter mocks in your [JUnit](http://junit.org) tests using the excellent
[testcontainers](http://testcontainers.org) library. This will enable your mocks to start/stop before/after your
tests run.

Here's a simple overview:

1. Follow the _testcontainers_ 'getting started' documentation for your project.
2. Add your mock configuration and mock data to your project (e.g. under `src/test/resources`).
3. Add a _testcontainers_ `GenericContainer` class rule to your JUnit test, for one of the Imposter Docker images (see _Docker_ section).
4. Configure your `GenericContainer` to mount the directory containing your configuration and data (e.g. `src/test/resources`) to `/opt/imposter/config`.
5. Configure your `GenericContainer` to wait for the `/system/status` HTTP endpoint to be accessible so your tests don't start before the mock is ready.

Now, when you run your test, your custom mock container will start, load your configuration and mock data, ready
for your test methods to use it!
