# OpenAPI request validation

This document describes how to validate HTTP requests against an OpenAPI specification when using the [OpenAPI plugin](./openapi_plugin.md).

## Validating requests against the specification

Imposter allows you to validate your HTTP requests to ensure they match the OpenAPI specification.

To enable this, set the `validation.request` configuration option to `true`:

```yaml
# validating-request-config.yaml
---
plugin: "openapi"
specFile: "example-spec.yaml"

validation:
  request: true
```

Now, for every incoming request to a valid combination of path and HTTP method, Imposter will validate the request parameters, headers and body against the corresponding part of the specification.

If a request fails validation, Imposter logs the validation errors then responds with an HTTP 400 status and, optionally, a report of the errors.

For example, let's make an HTTP request to an endpoint whose specification requires a request body and also requires a header, named 'X-Correlation-ID':

```shell
$ curl -v -X POST http://localhost:8080/pets
```

> Note that our request does not provide either a request body or header.

This results in the following log entries:

```
WARN  i.g.i.p.o.s.SpecificationServiceImpl - Validation failed for POST /pets: Validation failed.
[ERROR][REQUEST][POST /pets @header.X-CorrelationID] Header parameter 'X-CorrelationID' is required on path '/pets' but not found in request.
[ERROR][REQUEST][POST /pets @body] A request body is required but none found.
```

...and the following HTTP response:

```shell
HTTP/1.1 400 Bad Request
Content-Type: text/plain
Content-Length: 261

Request validation failed:
[ERROR][REQUEST][POST /pets @header.X-CorrelationID] Header parameter 'X-CorrelationID' is required on path '/pets' but not found in request.
[ERROR][REQUEST][POST /pets @body] A request body is required but none found.
```

This is because in the corresponding part of the OpenAPI specification, both the header and request body are marked as required:

```yaml
/pets/{petId}:
  put:
    summary: Update a specific pet
    operationId: updatePet
    parameters:
      - in: path
        name: petId
        required: true
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/Pet" 
```

Note that if the request body were provided, its structure would be validated against the corresponding schema entry.

See [examples/openapi/request-validation](https://github.com/outofcoffee/imposter/blob/master/docs/examples/openapi/request-validation) for a working example.

## Configuring issue behaviour

When validation issues occur, the possible behaviours are:

- fail the request (`fail` or `true`)
- log only (`log`)
- ignore (`ignore` or `false`)

### Fail the request

```yaml
# fail the request if validation issues occur
validation:
  request: fail
```

### Log only

```yaml
# just log if validation issues occur
validation:
  request: log
```

### Ignore

```yaml
# ignore validation issues
validation:
  request: ignore
```

If the `validation` block is not specified, validation issue behaviour is controlled by the `IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR` environment variable. The default value is `ignore`. Possible values are the same as the configuration file (above).

## Configuring validation levels

You can control which validation checks are considered errors, and which are ignored.

To do this, use the `validation.levels` section, for example:

```yaml
# validating-request-config.yaml
---
plugin: "openapi"
specFile: "example-spec.yaml"

validation:
  request: true
  levels:
    validation.request.body.missing: WARN
    validation.request.security.invalid: ERROR
```

In the example above, if the request body were missing, Imposter would not treat this as a validation error.

The `validation.levels` block is a map of validation key (i.e. a type of validation check) to level (`ERROR` or `WARN`).

> See the full list of validations at the [swagger-request-validator-core project](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-core/src/main/resources/swagger/validation/messages.properties).
