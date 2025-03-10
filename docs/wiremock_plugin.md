# WireMock plugin

* Plugin name: `wiremock`
* Plugin class: `io.gatehill.imposter.plugin.wiremock.WiremockPluginImpl`

## Features

- supports setting request headers, query parameters, and HTTP method.
- supports response status code, response headers and response files (including templating).
- supports `body` responses.
- supports `jsonBody` responses.
- supports multiple body patterns.
- partial support for faults.
- supports plain (root object) and multiple (root array) mappings files.

## Install plugin

### Option 1: Using the CLI

To use this plugin, install it with the [Imposter CLI](./run_imposter_cli.md):

    imposter plugin install -d wiremock

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Install the plugin manually

To use this plugin, download the `imposter-plugin-wiremock.jar` JAR file from the [Releases page](https://github.com/imposter-project/imposter-jvm-engine/releases).

Enable it with the following environment variables:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Using the plugin

> Read the [Configuration](configuration.md) section to understand how to configure Imposter.

## Example

For working examples, see [WireMock examples](https://github.com/imposter-project/examples/tree/main/wiremock).

Let's assume your configuration is in a folder named `wiremock-simple`.

CLI example:

    imposter up ./wiremock-simple --engine-type docker-all

Docker example:

    docker run -ti -p 8080:8080 \
        -v $PWD/wiremock-simple:/opt/imposter/config \
        outofcoffee/imposter-all

Standalone Java example:

    java -jar distro/all/build/libs/imposter-all.jar \
        --configDir ./wiremock-simple

This starts a mock server using the WireMock plugin. Responses are served based on the mappings files inside the `wiremock-simple/mappings` folder and the templated response files in the `wiremock-simple/__files` folder.

Using the example above, Imposter will parse the WireMock mappings files and create an HTTP mock on [http://localhost:8080/](http://localhost:8080/).

### Testing the mock

```bash
curl http://localhost:8080/example1 -H 'Accept: application/json'

{ "id": 2, "name": "Dog" }
```

## Additional script context objects

There are no additional script context objects available.

## Further reading

- [WireMock examples](https://github.com/imposter-project/examples/tree/main/wiremock)
