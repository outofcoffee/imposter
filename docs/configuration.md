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

**Note:** You must specify the plugin to use in the configuration file. See the list of [plugins](index.md#Plugins) for possible values.

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

The [REST plugin](./rest_plugin.md) allows you to specify multiple resources, using the `resources` array. Each resource can have its own path, method, response behaviour etc.

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

#### Default values

Default values for response configuration are as follows:

| Field                 | Plugins(s)    | Type                  | Default                                            | Example                             |
|-----------------------|---------------|-----------------------|----------------------------------------------------|-------------------------------------|
| `contentType`         | all           | String                | `application/json`, or determined from static file | `text/plain`                        |
| `method`              | rest          | String (HTTP method)  | `GET`                                              | `POST`                              |
| `response.statusCode` | openapi, rest | Integer (HTTP status) | `200`                                              | `201`                               |
| `response.staticData` | openapi, rest | String                | empty                                              | `hello world`                       |
| `response.staticFile` | all           | String                | empty                                              | `data.json`                         |
| `response.headers`    | openapi, rest | Map of String:String  | empty                                              | `X-Custom-Header: value`            |

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

## Security

Imposter can require specific header values to authenticate incoming HTTP requests. [Read about how to do this](./security.md).

## Scripted responses (advanced)

For more advanced scenarios, you can also control Imposter's responses using JavaScript or Groovy scripts.

See the [Scripting](scripting.md) section for more information.
