# REST plugin

* Plugin name: `rest`
* Plugin class: `io.gatehill.imposter.plugin.rest.RestPluginImpl`

## Features

* Supports RESTful or plain HTTP APIs.
* Supports arbitrary format static files.
* Supports optional JSON array responses.

# Configuration

Read the [Configuration](configuration.md) section to understand how to configure Imposter.

### Additional context objects

None.

## Example

Here is an example configuration file:

    # simple-example-config.yaml
    ---
    plugin: rest
    path: "/example"
    response:
      staticFile: example-data.json

In this example, we are using a static response file (`example-data.json`) containing the following:

    {
      "hello": "world"
    }

A few things to call out:

* We’ve defined the endpoint `/cats` to return the contents of our sample JSON file; in other words an array of cats.
* We’ve also said that, because the response file is a JSON array, we want to allow querying of individual items by their ID, under the `/cats/:id` endpoint.
* This example assumes you’ve named the file containing your JSON array `cats.json` and that it is in the same directory as the configuration file.

### Start Imposter with the REST plugin

Let's assume your configuration is in the directory: `docs/examples/rest/simple`.

Docker example:

    docker run --rm -ti -p 8080:8080 \
        -v $(pwd)/docs/examples/rest/simple:/opt/imposter/config \
        outofcoffee/imposter-rest

Standalone Java example:

    java -jar distro/rest/build/libs/imposter-rest.jar \
        --configDir ./docs/examples/rest/simple

Send an HTTP request to the `/example` path defined in the configuration file to see the example response:

    $ curl -v http://localhost:8080/example
    ...
    HTTP/1.1 200 OK
    ...
    {
      "hello": "world"
    }

Once you're finished, stop the server with CTRL+C.

### Multiple paths

We can configure different responses at multiple paths as follows:

    # multi-response-config.yaml
    ---
    plugin: rest
    contentType: application/json
    resources:
      - path: "/cats/:id"
        type: array
        response:
          staticFile: cats.json
      - path: "/dogs/:id"
        type: array
        response:
          staticFile: dogs.json

Let's return an array of data at each endpoint:

    # cats.json
    [
      {
        "id": 1,
        "name": "Fluffy",
        "type": "Persian"
      },
      {
        "id": 2,
        "name": "Leo",
        "type": "Bengal"
      }
    ]

    # dogs.json
    [
      {
        "id": 1,
        "name": "Rex",
        "type": "Labrador"
      },
      {
        "id": 2,
        "name": "Fido",
        "type": "Husky"
      }
    ]

Start the server:

    docker run --rm -ti -p 8080:8080 \
        -v $(pwd)/docs/examples/rest/multiple:/opt/imposter/config \
        outofcoffee/imposter-rest

Send an HTTP request to the `/cats/1` path defined in the configuration file to see the first item in the array:

    $ curl -v http://localhost:8080/cats/1
    ...
    HTTP/1.1 200 OK
    ...
    {
      "id": 1,
      "name": "Fluffy",
      "type": "Persian"
    }

Once you're finished, stop the server with CTRL+C.

> For more working examples, see:
>
> * docs/examples/rest
> * plugin/rest/src/test/resources/config

## Scripted responses (advanced)

For simple scenarios, use the `staticFile` property within the `response` object in your configuration.

For more advanced scenarios, you can also control Imposter's responses using JavaScript or Groovy scripts.

See the [Scripting](scripting.md) section for more information.
