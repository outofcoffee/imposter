# Capturing request data

Imposter allows you to capture elements of the request. You can use these elements in a [response template](./templates.md), a [script](./scripting.md) or add them to a [store](./stores.md) for later use.

It is possible to capture the following elements into a store:

- path parameter
- query parameter
- request header
- part or all of the request body (using JsonPath or XPath expression)

## Capture example

Use the `capture` block of a resource, as follows:

```yaml
resources:
- path: "/users/:userName"
  method: PUT
  capture:
    user:
      pathParam: userName
      store: testStore
```

In this example, the value of the path parameter 'userName' is added to the store named 'testStore' as an item named 'user'.

For example, the following request:

```
PUT /users/alice
```

...would result in the 'testStore' store containing the item 'user' with the value 'alice'.

Note that the name of the item is the object key - in the above example it is `user`. Multiple items can be captured from the same request using different keys:

```yaml
resources:
- path: "/users/:userName"
  method: PUT
  capture:
    user:
      pathParam: userName
      store: testStore
    agent:
      requestHeader: User-Agent
      store: testStore
```

## Capture configuration

The following configuration options are available for a capture:

| Element           | Purpose                                                                                                                                                       |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| capture block key | The name of the item to capture, e.g. `user`.                                                                                                                 |
| `store`           | The name of the store in which to put the item.                                                                                                               |
| `pathParam`       | The name of the path parameter to capture. Must reference the resource path, e.g. `userId` for a path of `/users/:userId`                                     |
| `queryParam`      | The name of the query parameter to capture.                                                                                                                   |
| `requestHeader`   | The name of the request header to capture.                                                                                                                    |
| `expression`      | A placeholder expression, e.g. `${context.request.queryParams.foo}` - see _Expressions_ section.                                                              |
| `const`           | A constant value, e.g. `example`.                                                                                                                             |
| `phase`           | The point in the request processing lifecycle that capture and persistence will occur. By default this is `REQUEST_RECEIVED`. See _Deferred capture_ section. |                                                                                         |
| `jsonPath`        | The JsonPath expression to query the JSON body. Only works with JSON request bodies.                                                                          |
| `xPath`           | The XPath expression to query the XML body. Only works with XML request bodies.                                                                               |
| `xmlNamespaces`   | Map of prefixes to XML namespaces used by the XPath expression.                                                                                               |

### Capturing the request body

You can capture part or all of the request body using a JsonPath or XPath expression.

#### JsonPath example

For example, if the request body was:

```json
{
  "name": "Alice",
  "address": {
    "street": "1 Main Street",
    "postCode": "PO5 7CO"
  }
}
```

...you could capture the name as follows:

```yaml
# part of your configuration file

resources:
- path: "/users"
  method: POST
  capture:
    firstName:
      jsonPath: $.name
      store: testStore
```

In this example, the `name` property of the body would be stored in the 'firstName' item in the store named 'testStore'.

#### XPath example

For example, if the request body was:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/">
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
```

...you could capture the value of the `pets:name` element as follows:

```yaml
# part of your configuration file

resources:
- path: "/users"
  method: POST
  capture:
    petName:
      xPath: "/env:Envelope/env:Body/pets:animal/pets:name"
      store: testStore
      xmlNamespaces:
        env: "http://schemas.xmlsoap.org/soap/envelope/"
        pets: "urn:com:example:petstore"
```

In this example, the value of the `pets:name` element in the body would be stored in the 'petName' item in the store named 'testStore'.

> Note: although this example uses a SOAP envelope, any valid XML body can be captured.

### Expressions

You can use an expression in a key name or value.

For example:

    ${context.request.headers.Correlation-ID}

Composite expressions are also supported:

    example_${context.request.headers.Correlation-ID}_${context.request.headers.User-Agent}

> Note the mix of placeholders, like `${context.request.headers.Correlation-ID}`, and plain text in this example.

The following expressions are supported:

| Expression format                       | Example                                    | Example value                |
|-----------------------------------------|--------------------------------------------|------------------------------|
| `context.request.body`                  | `${context.request.body}`                  | `{ "example request" }`      |
| `context.request.headers.HEADERNAME`    | `${context.request.headers.User-Agent}`    | `"Mozilla"`                  |
| `context.request.pathParams.PARAMNAME`  | `${context.request.pathParams.account}`    | `"example"`                  |
| `context.request.queryParams.PARAMNAME` | `${context.request.queryParams.page}`      | `"1"`                        |
| `context.response.body`                 | `${context.response.body}`                 | `{ "example response" }`     |
| `context.response.headers.HEADERNAME`   | `${context.response.headers.Content-Type}` | `"application/json"`         |
| `datetime.now.iso8601_date`             | `${datetime.now.iso8601_date}`             | `"2022-01-20"`               |
| `datetime.now.iso8601_datetime`         | `${datetime.now.iso8601_datetime}`         | `"2022-01-20T14:23:25.737Z"` |
| `datetime.now.millis`                   | `${datetime.now.millis}`                   | `"1642688570140"`            |
| `datetime.now.nanos`                    | `${datetime.now.nanos}`                    | `"30225267785430"`           |

Example:

```yaml
# part of your configuration file

resources:
  - path: "/people/:team/:person"
    method: POST
    capture:
      personInTeam:
        expression: "person=${context.request.pathParams.person},team=${context.request.pathParams.team}"
        store: testStore
```

For a request such as the following:

    GET /people/engineering/jane

The captured item, named `personInTeam`, would have the value: `"person=jane,team=engineering"`

#### Response expressions

Response expressions, such as `${context.response.body}`, must use _Deferred capture_ (see Deferred capture section).

Example:

```yaml
# part of your configuration file

resources:
- path: "/example"
  method: GET
  capture:
    responseBody:
      expression: "${context.response.body}"
      store: testStore
      phase: RESPONSE_SENT # this is required for response capture
```

### Capturing an object

In some scenarios you may wish to capture an object instead of a single value.

For example, to capture the address from the example above, use the JsonPath expression `$.address` - this will result in the entire address object being captured.

You can retrieve this object in a script, by accessing the [store](./stores.md) named 'testStore', or you could use it in a JsonPath placeholder within a [template](./templates.md).

### Constant values

In some scenarios, you may wish to capture a constant value.

Example:

```yaml
plugin: rest

resources:
- method: GET
  path: /test
  capture:
    # constant value
    receivedRequest:
      store: example
      const: received
  response:
    statusCode: 200
```

In the example above, the value `received` is stored in the 'example' store, with the name 'receivedRequest', when the given endpoint is hit.

### Dynamic item names

You do not have to specify a constant value for the item name - you can use a property of the request, such as a query or path parameter, header or body element as the item name.

Dynamic item names are useful when you want to capture collections of items, each with their own name derived from the request.

Example:

```yaml
plugin: rest

resources:
- method: PUT
  path: /users/admins/:userId
  capture:
    adminUser:
      expression: "${datetime.now.iso8601_datetime}"
      key:
        pathParam: userId
      store: adminUsers
  response:
    statusCode: 200
```

In the example above, an item corresponding to the `userId` parameter in the request is added to the 'adminUsers' store with the value of the current date/time.

## Request scoped store

There is a special request-scoped store, named `request`, which is accessible only to the current request. Its contents do not persist beyond the lifecycle of the request.

The request scoped store is very useful when you need to capture an item for immediate use, such as in a response template, but you don't need to persist it for later use.

Here is an example combining capture and response template:

> Learn more about [response templates](templates.md).

```yaml
# part of your configuration file

resources:
  - path: "/users/:userName"
    method: PUT
    capture:
      user:
        pathParam: userName
        store: request
    response:
      file: example-template.json
      template: true
```

Here is the corresponding template file:

```
{
  "userName": "${request.user}"
}
```

If you were to make the following request:

```
curl -X PUT http://localhost:8080/users/alice
```

...you would receive the following response:

```json
{
  "userName": "alice"
}
```

## Deferred capture

If you do not need an item to be persisted to the store immediately, you can choose to _defer_ capture. This will result in the capture and persistence operation being triggered _after_ processing of the current request has completed and the response has been transmitted to the client.

Deferring capture has the advantage of improving request throughput, at the cost of persistence occurring after the request has been completed. This trade-off may be useful for particular use cases, such as when writing events to a store for later retrieval, where real-time access is not required.

### Important considerations

Deferred items will not be available in the current request (such as in response templates or scripts). Given that the actual persistence operation runs asynchronously, there is no guarantee that it will complete before a subsequent request. When using deferred capture, you should consider carefully any dependent logic or configuration that expects the presence of an item in the store at a particular point in time.

Note that deferred capture cannot be used with the request scoped store, since the request store only applies to a single request.

### Configuring deferred capture

To enable deferred capture for a particular case, set the `phase: RESPONSE_SENT` property in a capture block, for example:

```yaml
# ...other configuration
capture:
  example:
    expression: "${context.request.queryParams.example}"
    store: testStore
    phase: RESPONSE_SENT
```

> The default value of `phase` is `REQUEST_RECEIVED`

## Enable or disable capture configuration

You can selectively enable or disable a capture configuration using the `enabled` key.

```yaml
# ...other configuration
capture:
  firstName:
    enabled: true
    jsonPath: $.name
    store: testStore
```

> The default value of `enabled` is `true`.

This can be helpful when used in conjunction with environment variable [placeholders](./configuration.md).

For example, if you set the environment variable:

    NAME_CAPTURE_ENABLED=true

You can refer to it in your configuration to selectively enable the capture configuration:

```yaml
# ...other configuration
capture:
  firstName:
    enabled: ${env.NAME_CAPTURE_ENABLED}
    jsonPath: $.name
    store: testStore
```

## Capture performance

Data capture incurs overhead on response times, depending on the speed of the store implementation used. If using the in-memory store, the performance impact is lower than using an external store. For store providers backed by external datastores, requests will incur a synchronous write to the store when capturing data.

You might consider using deferred capture, which has the advantage of improving request throughput, at the cost of persistence occurring after the request has been completed.

Using JsonPath to capture the request body is computationally expensive, as it requires parsing and querying of the request body item rather than just copying a reference.

## Examples

- [data-capture](https://github.com/outofcoffee/imposter/blob/main/examples/rest/data-capture)
