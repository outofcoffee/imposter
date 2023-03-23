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

### Request form parameters

Syntax: `context.request.formParams.<param name>`

Example config:

```yaml
# part of your configuration file

resources:
- path: /users
  method: POST
  response:
    content: "Hello ${context.request.formParams.user}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/users --data-urlencode "user=alice"

Hello alice
```

### Date/time values

Syntax: `datetime.now.<property>`

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

Valid date/time properties:

| Syntax                          | Example                                    | Example value                |
|---------------------------------|--------------------------------------------|------------------------------|
| `datetime.now.iso8601_date`     | `${datetime.now.iso8601_date}`             | `"2022-01-20"`               |
| `datetime.now.iso8601_datetime` | `${datetime.now.iso8601_datetime}`         | `"2022-01-20T14:23:25.737Z"` |
| `datetime.now.millis`           | `${datetime.now.millis}`                   | `"1642688570140"`            |
| `datetime.now.nanos`            | `${datetime.now.nanos}`                    | `"30225267785430"`           |

### Random values

Syntax: `random.<function>`

Example config:

```yaml
# part of your configuration file

resources:
- path: /now
  method: POST
  response:
    content: "An integer is ${random.numeric}"
    template: true
```

Example request:

```
$ curl http://localhost:8080/now

An integer is 42
```

Valid random functions:

| Syntax                  | Example                    | Example value                            |
|-------------------------|----------------------------|------------------------------------------|
| `random.alphabetic()`   | `${random.alphabetic()}`   | `"i"`                                    |
| `random.alphanumeric()` | `${random.alphanumeric()}` | `"i"` or `"42"`                          |
| `random.numeric()`      | `${random.numeric()}`      | `"42"`                                   |
| `random.uuid()`         | `${random.uuid()}`         | `"e1a4fba9-33eb-4241-84cf-472f90639c37"` |

> Note: the syntax for `random` is slightly different to the other placeholder types. These are functions, not properties, so are called with parentheses.

### Items in a Store

You can use items from a [Store](./stores.md), including data [captured](./data_capture.md) previously, or set by a script.

Syntax: `stores.<store name>.<item name>`

For example, a template file might look like this:

```
{
  "userName": "${stores.testStore.person}"
}
```

The placeholder `${stores.testStore.person}` refers to an item named 'person' in the store named 'testStore'.

> Learn more about [stores](./stores.md).

---

## Script-driven templates

When you are using [scripting](./scripting.md) to control mock behaviour, you can use the `template()` method, as follows:

```js
respond().withFile('example-template.json').template();
```

As with the configuration-driven approach described above, your template includes placeholders.

A common pattern is to use a script to retrieve items from a store, or generate values dynamically and set them in a store, for use in a template.

---

## Using JsonPath in placeholders

You can use a JsonPath expression to query an object when using a placeholder.

See the [template queries](template_queries.md) page for more details.

## Templating performance

Templating incurs a performance penalty, but is often faster than dynamically generating large objects using scripts, so is generally a better tradeoff when dynamic responses are required.

Template files are cached in memory once read from disk, so they do not incur as high an I/O cost from storage on subsequent requests.

## Examples

- [response-template](https://github.com/outofcoffee/imposter/blob/main/examples/rest/response-template)
