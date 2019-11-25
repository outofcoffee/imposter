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
if (context.request.params.action == 'create') {
    respond {
        withStatusCode 201
        immediately()
    }
}
```
    
We will explain this syntax later, in the _ResponseBehaviour object_ section. For now, it's enough to know that the example above causes the mock server to respond with HTTP status code 201 if the value of the `action` parameter in the request is `create`.

For example:

    HTTP GET http://localhost:8080/example-two?action=create
    ...
    201 Created

*Tip:* The `params` object used in the script is just a map of the request parameters, so you can use either `params.yourParamName` or `params['yourParamName']` syntax to access its members.

### Another example

Here's a more sophisticated example script:

```groovy
switch (context.request.params.action) {
    case 'create':
        // HTTP Status-Code 201: Created.
        respond {
            withStatusCode 201
            immediately()
        }
        break

    case 'fetch':
        // use a static response file and the default plugin behaviour
        respond {
            withFile 'example-data.json'
            and()
            usingDefaultBehaviour()
        }
        break

    default:
        // default to bad request
        respond {
            withStatusCode 400
            immediately()
        }
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

There are many other script objects you could use in order to decide what to return. For example, your script might use the request method (GET, POST, PUT, DELETE etc.) or other request attributes.

## Script objects

In order to help you determine what action to take, Imposter makes certain objects available to your scripts.

| Object    | Description                                                           |
|-----------|-----------------------------------------------------------------------|
| `context` | Convenience object for accessing request properties                   |
| `config`  | The plugin configuration for the current request                      |
| `logger`  | Logger, supporting levels such as `info(String)`, `warn(String)` etc. |

## Context object

The `context` object is available to your scripts. It holds things you might like to interrogate, like the request object.

| Property  | Description       |
|-----------|-------------------|
| `request` | The HTTP request. |

**Note:** Certain plugins will add additional properties to the `context`. For example, the _hbase_
plugin provides a `tableName` object, which you can use to determine the HBase table for the request being served.

## Request object

The request object is available on the `context`. It provides access to request parameters, method, URI etc.

| Property | Description                                | Example                                |
|----------|--------------------------------------------|----------------------------------------|
| `method` | The HTTP method of the request.            | `"GET"`                                |
| `uri`    | The absolute URI of the request.           | `"http://example.com?foo=bar&baz=qux"` |
| `params` | A `Map` containing the request parameters. | `[ "foo": "bar", "baz": "qux" ]`       |
| `body`   | A `String` containing the request body.    | `"Hello world."`                       |

## ResponseBehaviour object

Your scripts have access to the methods on `io.gatehill.imposter.script.MutableResponseBehaviour`.

The ResponseBehaviour object provides a number of methods to enable you to control the mock server response:

| Method                       | Description                                                    |
|------------------------------|----------------------------------------------------------------|
| `withStatusCode(int)`        | Set the HTTP status code for the response                      |
| `withFile(String)`           | Respond with the content of a static file                      |
| `withData(String)`           | Respond with the content of a `String`                         |
| `withHeader(String, String)` | Set a response header                                          |
| `withEmpty()`                | Respond with empty content, or no records                      |
| `usingDefaultBehaviour()`    | Use the plugin's default behaviour to respond                  |
| `immediately()`              | Skip the plugin's default behaviour and respond immediately    |
| `and()`                      | Syntactic sugar to improve readability of `respond` statements |

You structure your response behaviours like so:

```groovy
respond() // ... behaviours go here
```

For example:

```groovy
respond()
    .withStatusCode(201)
    .immediately();
```

Or:

```groovy
respond()
    .withFile('static-data.json')
    .and()
    .usingDefaultBehaviour();
```

*****
**Tip for Groovy users**

Groovy users can also use this special syntax, for improved readability:

```groovy
respond {
    // behaviours go here
}
```

For example:

```groovy
respond {
    withStatusCode 201
    and()
    usingDefaultBehaviour()
}
```

*****

### Returning data from a script

As we have seen above, to return data when using a script, you specify a response file.

More specifically, to specify which response file to use, you can either:

1. set the `staticFile` property within the `response` object in your configuration, which will be treated as the default, or
2. explicitly call the `withFile(String)` method in your script.

Here's an example of the static file approach:

```yaml
# file-example-config.yaml
---
plugin: rest
path: "/scripted"
contentType: application/json
response:
  scriptFile: example.groovy
  staticFile: example-data.json
```

Here, the response file `example-data.json` will be used, unless the script invokes the
`withFile(String)` method with a different filename.

In order for the mock server to return the response file in an appropriate format, the plugin must be allowed to process it. That means you should not call `immediately()` unless you want to skip using a response file (e.g. if you want to send an error code back or a response without a body).

Whilst not required, your script could invoke `usingDefaultBehaviour()` for readability to indicate that you want the plugin to handle the response file for you. See the *rest* plugin tests for a working example. To this end, the following blocks are semantically identical:

```groovy
respond {
    withFile 'static-data.json' and() usingDefaultBehaviour()
}
```

and:

```groovy
respond {
    withFile 'static-data.json'
}
```

### Setting response headers

You can set response headers using the `withHeader(String, String)` method. 

```groovy
respond {
    withHeader('X-Custom-Header', 'example value')
}
```
### Returning raw data

You can return raw data using the `withData(String)` method.

```groovy
respond {
    withData '{ "someKey": "someValue" }'
}
```
