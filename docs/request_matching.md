# Advanced request matching

You can match resources to requests using attributes such as the HTTP method, path, query string, headers or JSON body.

## Matching on HTTP method, path, query string or headers

See [Configuration](./configuration.md) for details.

## Matching on JSON body

You can match a resource based on a JsonPath query of a JSON request body.

> Only JSON request bodies are supported for the feature.

Specify the match configuration using the `requestBody` property of a resource.

Here you specify a JsonPath expression, and the value it must match.

For example:

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    jsonPath: $.foo
    value: bar
  response:
    statusCode: 204
```

This will match a request body like this:

```json
{ "foo": "bar" }
```

### Unmatched or null JsonPath expressions

If the result of evaluating the JsonPath expression is `null` or if the path evaluates to non-existent property in the body, then it is considered `null`.

You can explicitly match a `null` value, as follows:

```yaml
resources:
- method: GET
  path: /example2
  requestBody:
    jsonPath: $.not-matching-example
    value: null
  response:
    statusCode: 409
```

> Note: the YAML keyword `null` indicates a null value, not the string literal `"null"`
