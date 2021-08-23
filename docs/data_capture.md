# Capturing request data

Imposter allows you to capture elements of the request. You can use these elements in a [response template](./templates.md), a [script](./scripting.md) or add them to a [store](./stores.md) for later use.

It is possible to capture the following elements into a store:

- path parameter
- query parameter
- request header
- part or all of the request body (using JsonPath expression)

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

| Element           | Purpose                                                                                                                   |
|-------------------|---------------------------------------------------------------------------------------------------------------------------|
| capture block key | The name of the item to capture, e.g. `user`.                                                                             | 
| `store`           | The name of the store in which to put the item.                                                                           | 
| `pathParam`       | The name of the path parameter to capture. Must reference the resource path, e.g. `userId` for a path of `/users/:userId` | 
| `queryParam`      | The name of the query parameter to capture.                                                                               | 
| `requestHeader`   | The name of the request header to capture.                                                                                | 
| `jsonPath`        | The JsonPath expression to query the JSON body. Only works with JSON request bodies.                                      | 

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
      staticFile: example-template.json
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

## Capturing the request body

You can capture part or all of the request body using a JsonPath expression.

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

### Capturing an object

In some scenarios you may wish to capture an object instead of a single value.

For example, to capture the address from the example above, use the JsonPath expression `$.address` - this will result in the entire address object being captured.

You can retrieve this object in a script, by accessing the [store](./stores.md) named 'testStore', or you could use it in a JsonPath placeholder within a [template](./templates.md).

## Capture performance

Data capture can incur overhead on response times, depending on the speed of the store implementation used. If using the in-memory store, the performance impact is immaterial (with the caveat of JsonPath matching). For store providers backed by external datastores, requests will incur a synchronous write to the store when capturing data.

Using JsonPath to capture the request body is computationally expensive, as it requires parsing and querying of the request body item rather than just copying a reference.

## Examples

- [data-capture](./examples/rest/data-capture)