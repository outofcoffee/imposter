# Configuration guide

Read this section to learn how to configure Imposter.

## Basics

Imposter configuration files are in YAML or JSON format. They must be named with a `-config.yaml` or `-config.json` suffix. For example: `mymock-config.yaml`.

Here is an example configuration file:

```yaml
# simple-example-config.yaml
---
plugin: rest
path: "/example"
response:
  staticFile: example-data.json
```

Or, in JSON format:

```json
{
  "plugin": "rest",
  "path": "/example",
  "response": {
    "staticFile": "example-data.json"
  }
}
```

**Note:** You must specify the plugin to use in the configuration file. See the list of [plugins](./features_plugins.md) for possible values.

### Returning data

You can control the data Imposter sends back using response files. The response file is used by the active plugin to generate a response. For example, the [REST plugin](rest_plugin.md) might return the content of the file unmodified, whereas the [HBase](hbase_plugin.md) and [SFDC](sfdc_plugin.md) plugins use the response file to generate responses that mimic their respective systems.

Response files can be named anything you like; their path is resolved relative to the configuration file.

## Simple, static responses

For simple scenarios, use the `staticFile` property within the `response` object in your configuration.

In the example above, we are using a static response file (`example-data.json`) containing the following:

```json
{
  "hello": "world"
}
```

Using the configuration above, if we were to send an HTTP request to the `/example` path defined in the configuration file, we would see:

    HTTP GET http://localhost:8080/example
    ...
    HTTP/1.1 200 OK
    ...
    {
      "hello": "world"
    }

The plugin has returned the contents of the `staticFile` in the HTTP response.

Your response files can also be templated - that is, contain placeholders for substituted at runtime. See [templates](./templates.md) for more information.

### Response configuration options

You can specify other properties of the response, such as status code and headers. Here is a more complete example:

#### Single resource example

```yaml
# single-response-config.yaml
---
plugin: rest
path: "/example"
method: POST
contentType: "application/json"
response:
  staticFile: data.json
  statusCode: 201
  headers:
    X-Custom-Header: foo
```

A few things to call out:

* This endpoint will only be accessible via the `POST` HTTP method
* We've indicated that status code 201 should be returned
* We've set the content type of the response to JSON
* A custom header will be returned

#### Multiple resources example

The [OpenAPI plugin](./openapi_plugin.md) and [REST plugin](./rest_plugin.md) allow you to specify multiple resources, using the `resources` array. Each resource can have its own path, method, response behaviour etc.

```yaml
# multi-response-config.yaml
---
plugin: rest
resources:
- path: "/example1"
  contentType: "application/json"
  method: GET
  response:
    staticFile: data1.json
    statusCode: 200
    headers:
      X-Custom-Header: foo

- path: "/example2"
  contentType: "text/plain"
  method: POST
  response:
    statusCode: 201
    headers:
      X-Custom-Header: bar
    staticData: |
      This is some
      multiline response data.
```

##### Default response configuration

In some cases, you might want to define default response configuration, e.g. a header that should be sent in all responses. To do this, set the `defaultsFromRootResponse: true` option, as follows:

```yaml
plugin: rest

# root response config should be inherited
defaultsFromRootResponse: true

response:
  headers:
    X-Always-Present: Yes

resources:
- method: GET
  path: /example1
  response:
    staticData: "Hello world"

- method: GET
  path: /example2
  response:
    staticData: "Lorem ipsum"
```

In this example, responses to both `/example1` and `/example2` will have the header `X-Always-Present: Yes` set, as it is inherited from the root configuration.

> See [default-response-config](https://github.com/outofcoffee/imposter/blob/main/examples/rest/default-response-config) for an example.

#### Default response values

If unset by configuration or a script, the default values for response configuration fields are as follows:

| Field                 | Plugin(s)     | Type                  | Default                                            | Example                             |
|-----------------------|---------------|-----------------------|----------------------------------------------------|-------------------------------------|
| `contentType`         | all           | String                | `application/json`, or determined from static file | `text/plain`                        |
| `response.statusCode` | openapi, rest | Integer (HTTP status) | `200`                                              | `201`                               |
| `response.staticData` | openapi, rest | String                | empty                                              | `hello world`                       |
| `response.staticFile` | all           | String                | empty                                              | `data.json`                         |
| `response.headers`    | openapi, rest | Map of String:String  | empty                                              | `{ "X-Custom-Header": "value" }`    |

## Conditional responses

You can make Imposter respond with different values based on certain properties of the request in your configuration file, or using the script engine.

> For information about the script engine, see the [Scripting](./scripting.md) documentation.

Configure different response behaviours based on the following request attributes:

| Field            | Plugin(s)     | Type                                | Example                                        |
|------------------|---------------|-------------------------------------|------------------------------------------------|
| `path`           | all           | String                              | `/example/path`                                |
| `method`         | openapi, rest | String (HTTP method)                | `POST`                                         |
| `pathParams`     | openapi, rest | Map of String:String                | `{ "productCode": "abc" }`                     |
| `queryParams`    | openapi, rest | Map of String:String                | `{ "limit": "10" }`                            |
| `requestHeaders` | openapi, rest | Map of String:String                | `{ "User-Agent": "curl" }`                     |
| `requestBody`    | openapi, rest | Request body matching configuration | See [advanced matching](./request_matching.md) |

Here is an example showing a range of fields:

```yaml
plugin: openapi
specFile: apispec.yaml

resources:
  # handles GET /pets?page=1
  - path: "/pets"
    method: GET
    queryParams:
      page: 1
    response:
      statusCode: 200
  
  # handles GET /pets/10
  - path: "/pets/:petId"
    method: GET
    pathParams:
      petId: 10
    response:
      statusCode: 401
      staticData: "You do not have permission to view this pet."
  
  # handles PUT /pets/:petId with a request header 'X-Pet-Username: foo'
  - path: "/pets/:petId"
    method: PUT
    requestHeaders:
      X-Pet-Username: foo
    response:
      statusCode: 409
      staticData: "Username already exists."
```

### Matching the request body

You can also match resources based on the request body (both JSON and XML are supported). See [advanced matching](./request_matching.md) for details.

## Capturing data

Imposter allows you to capture elements of the request. You can use these elements in a [response template](./templates.md), a [script](./scripting.md) or add them to a [store](./stores.md) for later use. See [data capture](./data_capture.md) for more information.

## Environment variables

You can use environment variables as placeholders in plugin configuration files.

```yaml
# A plugin configuration using an environment variable.
---
plugin: rest
path: /example
response:
  staticData: "${env.EXAMPLE_RESPONSE}"
```

Here the environment variable `EXAMPLE_RESPONSE` will be substituted into the configuration. For example, if the variable was set as follows:

```
EXAMPLE_RESPONSE="Hello"
```

...then the static data `Hello` will be returned.

> You can use environment variables anywhere within your configuration files, for example, in the `security` section to avoid including secrets in your configuration files. 

### Default environment variable values

You can use the following syntax to set defaults for environment variables:

```
${env.NAME_OF_VAR:-defaultValue}
```

For example:

```
${env.MY_VAR:-foo}
```

This would resolve to the value `foo` if the `MY_VAR` environment variable was empty or missing.

## Security

Imposter can require specific header values to authenticate incoming HTTP requests. [Read about how to do this](./security.md).

## Config file discovery

By default, Imposter reads configuration files within the configuration directories, but not their subdirectories.

To also load configuration files within subdirectories, set the following environment variable:

    IMPOSTER_CONFIG_SCAN_RECURSIVE="true"

## Scripted responses (advanced)

For more advanced scenarios, you can also control Imposter's responses using JavaScript or Groovy scripts.

See the [Scripting](scripting.md) section for more information.

## Resource matching performance

Resource matching is typically the fastest method of providing conditional responses. This is the case for request properties such as headers, query parameters, path parameters, path and HTTP method. In the case of using [JsonPath to query the request body](./request_matching.md) to conditionally match resources, however, the body must be parsed, which is computationally expensive and will result in lower performance.
