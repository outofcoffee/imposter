# Scripted responses

Read this section to learn how to use scripts to configure Imposter's responses.

You can control Imposter's responses using JavaScript or Groovy scripts. (If you choose [Groovy](http://www.groovy-lang.org/), you can of course write plain Java in your scripts as well).

## Example

Here's an example configuration file that uses a script:

```yaml
# scripted-example-config.yaml
---
plugin: rest
path: "/example-two"
response:
  scriptFile: example.groovy
```

...and here's the corresponding script (`example.groovy`):

```groovy
if (context.request.queryParams.action == 'create') {
    respond().withStatusCode(201)
}
```

We will explain this syntax later, in the _ResponseBehaviour object_ section. For now, it's enough to know that the example above causes the mock server to respond with HTTP status code 201 if the value of the `action` parameter in the request is `create`.

For example:

    HTTP GET http://localhost:8080/example-two?action=create
    ...
    201 Created

> **Note**
> See this [rest plugin example](https://github.com/imposter-project/examples/blob/main/rest/conditional-scripted) for a simple example.

### Another example

Here's a more sophisticated example script:

```groovy
switch (context.request.queryParams.action) {
    case 'create':
        // HTTP Status-Code 201: Created.
        respond()
            .withStatusCode(201)
            .skipDefaultBehaviour()
        break

    case 'fetch':
        // use a static response file and the default plugin behaviour
        respond().withFile('example-data.json')
        break

    default:
        // default to bad request
        respond()
            .withStatusCode(400)
            .skipDefaultBehaviour()
        break
}
```

In this example, the script causes the mock server to respond with HTTP status codes 200, 201 or 400 depending on the value of the `action` parameter in the request.

For example:

    HTTP GET http://localhost:8080/example-two?action=fetch
    ...
    HTTP/1.1 200 OK
    ...
    {
      "hello": "world"
    }

In the case of `action=fetch`, the script causes the mock server to use the content of the static file
`static-data.json` to serve the response.

And:

    HTTP GET http://localhost:8080/example-two?action=foo
    ...
    400 Bad Request

In the default case, the script causes the mock server to return an HTTP 400 response, as shown above.

The `queryParams` object used in the script is just a map of the request query parameters, so you can use either `params.yourParamName` or `params['yourParamName']` syntax to access its members.

There are many other script objects you could use in order to decide what to return. For example, your script might use the request method (GET, POST, PUT, DELETE etc.) or other request attributes.

## Script objects

The following objects are available to your scripts, to help you determine what action to take.

| Object    | Description                                                                  |
|-----------|------------------------------------------------------------------------------|
| `context` | Parent object for accessing request properties                               |
| `config`  | The plugin configuration for the current request                             |
| `env`     | A map of environment variables, such as `{ "MY_VAR": "abc", "VAR2": "def" }` |
| `logger`  | Logger, supporting levels such as `info(String)`, `warn(String)` etc.        |

For example, if you want to access the directory containing the configuration file, you can use `config.dir`.

<details markdown>
<summary>JavaScript example</summary>

```js
console.log("Path to config dir: " + config.dir)

var someFile = config.dir + "/example.txt"
// use someFile...
```
</details>

<details markdown>
<summary>Groovy example</summary>

```groovy
logger.info("Path to config dir: " + config.dir)

def someFile = new java.io.File(config.dir, "example.txt")
// use someFile...
```
</details>

## Context object

The `context` object is available to your scripts. It holds things you might like to interrogate, like the request object.

| Property  | Description                     | Example                              |
|-----------|---------------------------------|--------------------------------------|
| `request` | The HTTP request.               | _See Request object._                |

**Note:** Certain plugins will add additional properties to the `context`. For example, the _hbase_
plugin provides a `tableName` object, which you can use to determine the HBase table for the request being served.

## Request object

The request object is available on the `context`. It provides access to request parameters, method, URI etc.

| Property            | Description                                                      | Example                                                   |
|---------------------|------------------------------------------------------------------|-----------------------------------------------------------|
| `path`              | The path of the request.                                         | `"/example"`                                              |
| `method`            | The HTTP method of the request.                                  | `"GET"`                                                   |
| `pathParams`        | A map containing the request path parameters.                    | `{ "productCode": "abc", "foo": "bar" }`                  |
| `queryParams`       | A map containing the request query parameters.                   | `{ "limit": "10", "foo": "bar" }`                         |
| `formParams`        | A map containing the request form parameters.                    | `{ "foo": "bar" }`                                        |
| `uri`               | The absolute URI of the request.                                 | `"http://example.com?foo=bar&baz=qux"`                    |
| `headers`           | A map containing the request headers.                            | `{ "X-Example": "ABC123", "Content-Type": "text/plain" }` |
| `normalisedHeaders` | A map containing the request headers with all keys in lowercase. | `{ "x-example": "ABC123", "content-type": "text/plain" }` |
| `body`              | A string containing the request body.                            | `"Hello world."`                                          |

> Note: keys are always lowercase in `normalisedHeaders`, regardless of the request header casing. This aids script portability, avoiding case-sensitivity for header keys.

## Response object

Your scripts have access to the methods on `io.gatehill.imposter.script.MutableResponseBehaviour`.

The response behaviour object provides a number of methods to enable you to control the response:

| Method                       | Plugin(s)     | Description                                                                                                         |
|------------------------------|---------------|---------------------------------------------------------------------------------------------------------------------|
| `withStatusCode(int)`        | all           | Set the HTTP status code for the response.                                                                          |
| `withFile(String)`           | all           | Respond with the content of a static file. Also see `template`.                                                     |
| `withContent(String)`        | all           | Respond with the literal content of a string. Also see `template`.                                                  |
| `withExampleName(String)`    | openapi       | Use with the [OpenAPI plugin](./openapi_plugin.md) to respond with a particular OpenAPI specification example.      |
| `withHeader(String, String)` | all           | Set a response header.                                                                                              |
| `withEmpty()`                | all           | Respond with empty content, or no records.                                                                          |
| `usingDefaultBehaviour()`    | all           | Use the plugin's default behaviour to respond.                                                                      |
| `skipDefaultBehaviour()`     | all           | Skip the plugin's default behaviour when responding.                                                                |
| `template()`                 | all           | Treat the response file or data as a [template](./templates.md) with placeholders.                                  |
| `continueToNext()`           | openapi, rest | Used by [interceptors](./interceptors.md) to indicate request processing should continue after the script executes. |
| `and()`                      | all           | Syntactic sugar to improve readability of `respond` statements.                                                     |

You structure your response behaviours like so:

```groovy
respond() // ... behaviours go here
```

For example:

```groovy
respond().withFile('static-data.json')
```

Or:

```groovy
respond()
    .withStatusCode(201)
    .withHeader("x-custom-header", "somevalue")
```

### Returning data from a script

To return data when using a script, you specify a response file or set the content to a string value.

When using a response file, you can either:

1. explicitly call the `withFile(String)` method in your script, or
2. set the `file` property within the `response` object in your configuration file, which will be used by default unless you override it.

Here's an example of setting the file from a script:

```groovy
respond().withFile('some-data.json')
```

Here's an example using the `file` property within the configuration file:

```yaml
# example-config.yaml
---
plugin: rest
path: "/scripted"
contentType: application/json
response:
  scriptFile: example.groovy
  file: example-data.json
```

In this example, the response file `example-data.json` will be used, unless the script invokes the
`withFile(String)` method with a different filename.

#### Response templates

Response files can be [templated](./templates.md), allowing you to populate placeholders with dynamic data.

### Returning raw response data

You can return response data using the `withContent(String)` method.

```groovy
respond().withContent('{ "someKey": "someValue" }')
```

Raw response content can also be [templated](./templates.md), allowing you to populate placeholders with dynamic data.

### Setting response headers

You can set response headers using the `withHeader(String, String)` method. 

```groovy
respond().withHeader('X-Custom-Header', 'example value')
```

### Returning a specific OpenAPI example

When using the [OpenAPI plugin](./openapi_plugin.md), you can return a specific named example from the specification using the `withExampleName(String)` method.

```groovy
respond().withExampleName('example1')
```

This selects the example from the OpenAPI `examples` section for the API response.

> **Note**
> See this [openapi plugin example](https://github.com/imposter-project/examples/blob/main/openapi/scripted-named-example) for a simple example.

### Using stores

You can read and write data in [Stores](./stores.md) using scripts.

### Overriding plugin behaviour

In advanced scenarios you can control the response processing behaviour of the mock plugin you're using. 

<details markdown>
<summary>Skipping plugin default behaviour</summary>

In order for the mock server to return the response file in an appropriate format, the plugin must be allowed to process it. This is the default behaviour after your script runs.

If you want to override the plugin's behaviour, you can call `skipDefaultBehaviour()` on the response (e.g. if you want to send an error code back).

```groovy
respond()
    .withStatusCode(400)
    .skipDefaultBehaviour()
```

The alternative to skipping the default behaviour is `usingDefaultBehaviour()`. Whilst not required, your script can invoke this for readability to indicate that you want the plugin to handle the response file for you.

The following blocks are semantically identical:

```groovy
respond()
    .withFile('static-data.json')
    .usingDefaultBehaviour()
```

and:

```groovy
respond().withFile('static-data.json')
```
</details>

## Further reading

* [Tips and tricks for JavaScript scripts](./javascript_tips.md)
* [Tips and tricks for Groovy scripts](./groovy_tips.md)
* [Debugging Groovy scripts](./groovy_debugging.md)
* [Using modern JavaScript in scripts](./scripting_modern_js.md)
