# Response templates

Imposter allows you to respond with a template - that is, a file containing placeholders, which are replaced with values at runtime.

Templates can be useful when you are [capturing data](./data_capture.md), using [stores](./stores.md) or generating data from [a script](./scripting.md).

Templates can be used with configuration or scripts.

## Key concepts

The template engine is enabled by setting `template: true` in a resource. For example:

```yaml
# part of your configuration file

resources:
- path: /example
  method: GET
  response:
    template: true
    file: some-template.txt
```

Templates can be specified as external files, or inline in configuration files.

Here is the same example as above, but using an inline `content` block:

```yaml
# part of your configuration file

resources:
- path: /example
  method: GET
  response:
    template: true
    content: |
      Hello ${context.request.pathParams.user}
      This is a templated response.
```

There are many placeholder types (see [placeholder types](#placeholder-types) section), including:

- properties of the request, such as path parameters, query parameters, headers or body
- date/time values, such as timestamps, epoc time in millis, nanos etc.
- data from [Stores](./stores.md), including data [captured](./data_capture.md) previously

## Placeholder types

This section details the placeholder types.

### Request path parameters

Syntax: `context.request.pathParams.<param name>`

Example config:

```yaml
# part of your configuration file

resources:
- path: /users/{user}
  method: GET
  response:
    content: "Hello ${context.request.pathParams.user}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/users/alice

Hello alice
```

### Request query parameters

Syntax: `context.request.queryParams.<param name>`

Example config:

```yaml
# part of your configuration file

resources:
- path: /users
  method: GET
  response:
    content: "Hello ${context.request.queryParams.user}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/users?user=alice

Hello alice
```

### Request headers

Syntax: `context.request.headers.<header name>`

Example config:

```yaml
# part of your configuration file

resources:
- path: /users
  method: GET
  response:
    content: "Hello ${context.request.headers.user}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/users -H "user: alice"

Hello alice
```

### Request body

Syntax: `context.request.body`

Example config:

```yaml
# part of your configuration file

resources:
- path: /users
  method: POST
  response:
    content: "Hello ${context.request.body}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/users --data "alice"

Hello alice
```

> Note: the use of curl's `--data` argument implies this is a `POST` request, with the value `alice` sent as the request body.

### Date/time values

Syntax: `datetime.now.[function]`

Example config:

```yaml
# part of your configuration file

resources:
- path: /now
  method: POST
  response:
    content: "The date is ${datetime.now.iso8601_date}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/now

The date is 2022-12-28
```

Valid date/time functions:

| Syntax                          | Example                                    | Example value                |
|---------------------------------|--------------------------------------------|------------------------------|
| `datetime.now.iso8601_date`     | `${datetime.now.iso8601_date}`             | `"2022-01-20"`               |
| `datetime.now.iso8601_datetime` | `${datetime.now.iso8601_datetime}`         | `"2022-01-20T14:23:25.737Z"` |
| `datetime.now.millis`           | `${datetime.now.millis}`                   | `"1642688570140"`            |
| `datetime.now.nanos`            | `${datetime.now.nanos}`                    | `"30225267785430"`           |

### Items in a Store

You can use items from a [Store](./stores.md), including data [captured](./data_capture.md) previously, or set by a script.

Syntax: `${STORE_NAME.ITEM_NAME}`

For example, a template file might look like this:

```
{
  "userName": "${testStore.person}"
}
```

The placeholder `${testStore.person}` refers to an item named 'person' in the store named 'testStore'.

> Learn more about [stores](./stores.md).

---

## Script-driven templates

When you are using [scripting](./scripting.md) to control mock behaviour, you can use the `template()` method, as follows:

```js
respond().withFile('example-template.json').template();
```

As with the configuration-driven approach described above, your template includes placeholders.

A common pattern is to use a script to retrieve items from a store, or generate values dynamically and set them in a store, for use in a template.

## Using JsonPath in placeholders

You can use a JsonPath expression to query a complex object in a placeholder.

This is useful if you have stored/captured an object, such as from a request body, and wish to use some part of the object instead of the whole object in a template.

The syntax is as follows:

```
${STORE_NAME.ITEM_NAME:JSONPATH_EXPRESSION}
```

For example:

```
${request.person:$.name}
```

In this example, there is quite a lot going on. First, the item named `person` is retrieved from the `request` store. Remember that when [capturing](./data_capture.md) data from the request, you specify the name of the item (in this case, 'person') and the source of the data. Our request body looks like this:

```json
{
  "name": "Alice",
  "occupation": "Programmer"
}
```

The corresponding capture configuration is as follows:

```yaml
# part of your configuration file

resources:
- path: "/users"
  method: POST
  capture:
    person:
      jsonPath: $
```

> Note that `$` indicates the whole request body object should be captured into the `person` item.

Since the `person` item is an object, we can use JsonPath to query the `name` property - hence the expression `$.name` in the template placeholder.

Similarly, you could refer to other properties of the item - `occupation` would look like this:

```
Your occupation is: ${request.person:$.occupation}
```

## Templating performance

Templating incurs a performance penalty, but is often faster than dynamically generating large objects using scripts, so is generally a better tradeoff when dynamic responses are required.

Template files are cached in memory once read from disk, so they do not incur as high an I/O cost from storage on subsequent requests.

Using JsonPath in placeholder templates is computationally expensive, as it requires parsing and querying of an item rather than just value substitution.

## Examples

- [response-template](https://github.com/outofcoffee/imposter/blob/main/examples/rest/response-template)
