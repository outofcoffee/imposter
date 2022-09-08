# Generate configuration (aka scaffolding)

You can get Imposter to create configuration files for you.

If you have an existing endpoint from which to record requests and responses, see the [proxy documentation](./proxy.md)

If you have an OpenAPI specification, Imposter can 'scaffold' a mock based on the resources and methods, it contains.

> If you don't have either of these, it's easy to create the configuration using [the guide](./configuration.md).

## Prerequisites

- Install the [Imposter CLI](./run_imposter_cli.md)

## Scaffolding from an OpenAPI specification

Let's start with a simple OpenAPI file:

```yaml
# petstore.yaml
openapi: "3.0.1"

info:
  title: Sample Petstore service
  version: "1.0.0"

paths:
  /pets:
    get:
      responses:
        '200':
          description: Returns all pets from the system
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  required:
                    - id
                    - name
                  properties:
                    id:
                      type: integer
                    name:
                      type: string
              examples:
                itemsExample:
                  value:
                    [
                      { "id": 101, "name": "Cat" },
                      { "id": 102, "name": "Dog" }
                    ]
```

Store this as a file named `petstore.yaml`.

Imposter can generate a configuration file from this specification:

    $ imposter scaffold
    
    INFO[0000] found 1 OpenAPI spec(s)
    DEBU[0000] generated 1 resources from spec
    INFO[0000] wrote Imposter config: /Users/mary/example/petstore-config.yaml

Look in the directory where you started Imposter and you will see a new file:

    $ ls -l
    -rw-r--r--  1 mary  wheel    79B  8 Sep 15:44 petstore-config.yaml
    -rw-r--r--  1 mary  wheel   800B  8 Sep 15:44 petstore.yaml

The `petstore-config.yaml` file is the Imposter configuration file:

```yaml
# petstore-config.yaml
plugin: openapi
specFile: petstore.yaml

resources:
  - method: GET
    path: /pets
```

Note that the HTTP method and path from the specification have been picked up. Since the `openapi` plugin is being used, when a request is made to this resource, the `examples` from the OpenAPI specification will be used.

### Testing the mock

In the same directory as the files above, start Imposter:

    $ imposter up

    14:51:51 INFO  i.g.i.Imposter - Starting mock engine 3.0.4
    14:51:51 DEBUG i.g.i.c.u.ConfigUtil - Loading configuration file: /opt/imposter/config/petstore-config.yaml
    14:51:51 DEBUG i.g.i.p.o.OpenApiPluginImpl - Adding mock endpoint: GET -> /pets
    14:51:51 DEBUG i.g.i.p.o.OpenApiPluginImpl - Adding specification UI at: http://localhost:8080/_spec
    14:51:51 INFO  i.g.i.Imposter - Mock engine up and running on http://localhost:8080

Imposter read the configuration files and a mock of the original endpoint is now running at `http://localhost:8080`

Call the mock:

    $ curl http://localhost:8080/pets

    [{"id":101,"name":"Cat"},{"id":102,"name":"Dog"}]

Imposter served the response based on what it captured.

    14:53:49 DEBUG i.g.i.h.AbstractResourceMatcher - Matched resource config for GET http://localhost:8080/pets
    14:53:49 DEBUG i.g.i.p.o.OpenApiPluginImpl - Setting content type [application/json] from specification for GET http://localhost:8080/pets
    14:53:49 INFO  i.g.i.p.o.s.ResponseTransmissionServiceImpl - Serving mock example for GET http://localhost:8080/pets with status code 200 (response body 49 bytes)

### Making changes

You can, of course, edit the configuration file so the mock behaves differently. When you change either the configuration file or response file, the Imposter CLI will restart to reflect your latest changes.

## What's next

Learn how to use Imposter with the [Configuration guide](configuration.md).
