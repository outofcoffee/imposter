# Configuration guide

Read this section to learn how to configure Imposter.

## Basics

Imposter configuration files must be named with a `-config.json` suffix. For example: `mydata-config.json`.

Here is an example configuration file:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/example",
      "response": {
        "staticFile": "example-data.json"
      }
    }

**Note:** You must specify the plugin to use in the configuration file. See the examples in this document for possible values.

## Simple responses (static response files)

You can control Imposter's responses using static response files. Use the `staticFile` property
within the `response` object in your configuration.

Response files can be named anything you like and their path is resolved relative to the server's configuration directory.

In the example above, we are using a static response file (`example-data.json`):

     {
       "hello": "world"
     }

## Scripted responses (advanced)

You can also control Imposter's responses using [JavaScript](https://www.javascript.com/) or
[Groovy](http://www.groovy-lang.org/) scripts. If you choose Groovy, you can of course write plain
Java in your scripts as well.

Here's an example configuration file that uses a script:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/scripted",
      "response": {
        "scriptFile": "example.groovy"
      }
    }

Here's the corresponding script (`example.groovy`):

    if (context.request.params["action"] == "create") {
        respond {
            withStatusCode 201
            immediately()
        }
    }

In the example above, the script causes the mock server to respond with HTTP status code 201 if the value of
the `action` parameter in the request is `create`.

Here's a more sophisticated example:

    switch (context.request.params["action"]) {
        case "create":
            // HTTP Status-Code 201: Created.
            respond {
                withStatusCode 201
                immediately()
            }
            break

        case "fetch":
            // use a different static response file with the default behaviour
            respond {
                withFile "static-data.json"
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

In this example, the script causes the mock server to respond with HTTP status codes 200, 201 or 400 depending on
the value of the `action` parameter in the request.

For example:

    HTTP GET http://localhost:8443/scripted?action=201
    ...
    201 Created

    HTTP GET http://localhost:8443/scripted?action=foo
    ...
    400 Bad Request

In the case of `action=fetch`, the script causes the mock server to use the content of the static file
`static-data.json` to serve the response.

## Script objects

Certain objects are available to your scripts.

| Object | Description
| --- | ---
| `context` | Convenience object for accessing request properties
| `config` | The plugin configuration for the current request
| `logger` | Logger, supporting levels such as `info(String)`, `warn(String)` etc.

## The context object

The `context` object is available to your scripts. It holds things you might like to interrogate,
like the request object.

| Property | Description
| --- | ---
| `request` | The HTTP request.

**Note:** Certain plugins will add additional properties to the `context`. For example, the _hbase_
plugin provides a `tableName` object, which you can use to determine the HBase table for the request being served.

## The request object

The request object is available on the `context`. It provides access to request parameters, method, URI etc.

| Property | Description | Example
| --- | --- | --------------------------------------
| `method` | The HTTP method of the request. | `"GET"`
| `uri` | The absolute URI of the request. | `"http://example.com?foo=bar&baz=qux"`
| `params` | A `Map` containing the request parameters. | `[ "foo": "bar", "baz": "qux" ]`
| `body` | A `String` containing the request body. | `"Hello world."`

## The ResponseBehaviour object

Your scripts are a subclass of `com.gatehill.imposter.script.AbstractResponseBehaviour`.

The ResponseBehaviour class provides a number of methods to enable you to control the mock server response:

| Method | Description
| --- | ---
| `withStatusCode(int)` | Set the HTTP status code for the response
| `withFile(String)` | Respond with the content of a static file
| `withEmpty()` | Respond with empty content, or no records
| `usingDefaultBehaviour()` | Use the plugin's default behaviour to respond
| `immediately()` | Skip the plugin's default behaviour and respond immediately
| `and()` | Syntactic sugar to improve readability of `respond` statements

Typically you structure your response behaviours like so:

    respond {
        // behaviours go here
    }

For example:

    respond {
        withStatusCode 201
        and()
        usingDefaultBehaviour()
    }

## Returning data

To return data when using a script, you specify a response file. To specify which response file to use, you can either:

1. set the `staticFile` property within the `response` object in your configuration, or
2. call the `ResponseBehaviour.withFile(String)` in your script.

The response file is used by the active plugin to generate a response. For example, the _rest_ plugin might return
the content of the file unmodified, however, the _hbase_ and _sfdc_ plugins use the response file to generate
responses that mimic their respective systems.

Here's an example of the static file approach:

    {
      "plugin": "com.gatehill.imposter.plugin.rest.RestPluginImpl",
      "path": "/scripted",
      "response": {
        "scriptFile": "example.groovy",
        "staticFile": "example-data.json"
      },
      "contentType": "application/json"
    }

Here, the response file `example-data.json` will be used, unless the script invokes the
`ResponseBehaviour.withFile(String)` method with a different filename.

In order for the mock server to return the response file in an appropriate format, the plugin must be allowed to
process it. That means you should not call `ResponseBehaviour.immediately()` unless you want to skip using a response file (e.g. if
you want to send an error code back or a response without a body).

Whilst not required, your script could invoke `ResponseBehaviour.usingDefaultBehaviour()` for readability to indicate
that you want the plugin to handle the response file for you. See the *rest* plugin tests for a working example. To this
end, the following blocks are semantically identical:

    respond {
        withFile "static-data.json" and() usingDefaultBehaviour()
    }

and:

    respond {
        withFile "static-data.json"
    }

## TLS/SSL

You can run Imposter with HTTPS enabled. To do this, enable the TLS option and provide keystore options.

### Example

    java -jar distro/build/libs/imposter.jar \
            --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
            --configDir /path/to/config \
            --tlsEnabled \
            --keystorePath ./server/src/main/resources/keystore/ssl.jks \
            --keystorePassword password

**Note:** This uses a self-signed certificate for TLS/SSL. You can also choose your own keystore.
If you need to trust the self-signed certificate when using the default, the keystore is located at
`server/src/main/resources/keystore` and uses the secure password 'password'.
