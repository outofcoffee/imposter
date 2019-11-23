# OpenAPI (aka Swagger) plugin

* Plugin name: `openapi`
* Plugin class: `com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl`

The plugin provides support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications.

## Features

* Creates mock endpoints from OpenAPI/Swagger v2 and OpenAPI v3 API specifications.
* Serves response examples embedded in the specification.
* Also supports static response files and script-driven responses, using status code, response files etc.
* Provides an interactive API sandbox at `/_spec`

# Configuration

Read the [Configuration](configuration.md) section to understand how to configure Imposter.

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `operation` | `io.swagger.models.Operation` | The OpenAPI operation for the request.

## Using the plugin

A great way to use this plugin is to take advantage of the built in `examples` feature of OpenAPI/Swagger files. These provide a standard way to document sample responses for each API response. This plugin will match the example to serve using a combination of:

* matching URI/path
* matching content type in `Accept` HTTP request header to the `produces` property of the response
* matching status code to the response

Typically you will use a simple script (see `plugin/openapi/src/test/resources/config` for working example) to control the status code, and thus the content of the response.

You can also use the interactive API sandbox at `/_spec`; e.g. [http://localhost:8080/_spec](http://localhost:8080/_spec), which looks like this:

![API sandbox](images/api-sandbox.png)

## Example

For working examples, see:

    plugin/openapi/src/test/resources/config

Let's assume your configuration is in a folder named `config`.

Docker example:

    docker run -ti -p 8080:8080 \
        -v $(pwd)/config:/opt/imposter/config \
        outofcoffee/imposter-openapi

Standalone Java example:

    java -jar distro/openapi/build/libs/imposter-openapi.jar \
        --plugin openapi \
        --configDir ./config

This starts a mock server using the OpenAPI plugin. Responses are served based on the configuration files inside the `config` folder; in particular the Swagger specification `petstore-expanded.yaml`.

Using the example above, you can interact with the APIs with examples in the Swagger specification at their respective endpoints under `http://localhost:8080/<endpoint path>`.

For specific information about the endpoints, see the interactive sandbox at [http://localhost:8080/_spec](http://localhost:8080/_spec).

## Object response examples

Imposter has limited support for response examples defined as objects, for example an API specification like [object-examples.yaml](../plugin/openapi/src/test/resources/config/object-examples.yaml).

The salient part of the response is as follows:

```yaml
responses:
  "200":
    description: team response
    schema:
      type: object
      items:
        $ref: '#/definitions/Team'
    examples:
      application/json:
        id: 10
        name: Engineering
```

> Note: the JSON example is specified as an object.

Imposter currently supports JSON and YAML serialised content types in the response if they are specified in this way. If you want to return a different format, return a literal string, such as those above.
