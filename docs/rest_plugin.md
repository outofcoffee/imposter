# REST plugin

Plugin class: `com.gatehill.imposter.plugin.rest.RestPluginImpl`

## Features

* Supports RESTful or plain HTTP APIs.
* Supports arbitrary format static files.
* Supports optional JSON array responses.

# Configuration

Read the [Configuration](configuration.md) section to understand how to configure Imposter.

### Additional context objects

None.

## Example

For working examples, see:

    plugin/rest/src/test/resources/config

Let's assume your configuration is in a folder named `config`.

Docker example:

    docker run -ti -p 8443:8443 \
        -v $(pwd)/config:/opt/imposter/config \
        outofcoffee/imposter-rest

Standalone Java example:

    java -jar distro/build/libs/imposter.jar \
        --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
        --configDir ./config

This starts a mock server using the simple REST plugin. Responses are served based on the configuration files
inside the `config` folder. With the example above, you can hit the URL
[http://localhost:8443/example](http://localhost:8443/example) to see the mock response.
