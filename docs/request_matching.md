# Advanced request matching

You can match resources to requests using attributes such as the HTTP method, path, query string, headers or body. For JSON and XML request bodies, JsonPath and XPath can also be used, respectively.

## Match operators

Matchers support a number of operators to control how the match is performed.

For example:

```yaml
resources:
- method: GET
  path: /example
  requestHeaders:
    content-type:
      value: somevalue
      operator: Contains
  response:
    statusCode: 400
```

The following operators are supported:

| Operator      | Description                                                                                           |
|---------------|-------------------------------------------------------------------------------------------------------|
| `EqualTo`     | Checks if the expression result equals the `value`.                                                   |
| `NotEqualTo`  | Checks if the expression result does not equal the `value`.                                           |
| `Exists`      | Checks if the expression result is not `null` or absent.                                              |
| `NotExists`   | Checks if the expression result is `null` or absent.                                                  |
| `Contains`    | Checks if the expression result contains the `value`.                                                 |
| `NotContains` | Checks if the expression result does not contain the `value`.                                         |
| `Matches`     | Checks if the expression result matches the regular expression specified in the `value` field.        |
| `NotMatches`  | Checks if the expression result does not match the regular expression specified in the `value` field. |

> **Note**
> If no `operator` is specified, then `EqualTo` is used.

---

## Matching against HTTP method, path, query string or headers

See [Configuration](./configuration.md) for simple matching against HTTP method, path, query string or headers.

If you need more complex matching, you can use the 'long form' configuration, which allows you to specify a match expression and operator for each attribute.

For example:

```yaml
resources:
- method: GET
  path: /example
  requestHeaders:
    content-type:
      value: somevalue
      operator: Contains
  response:
    statusCode: 400
```

Request matchers support the range of operators described in this document.

## Matching against the request body

You can also match a resource based on the request body. For JSON and XML request bodies, JsonPath and XPath can also be used, respectively.

### Matching a JSON request body

You can match a resource based on a JsonPath query of a JSON request body.

> Only JSON request bodies are supported for the feature.

Specify the match configuration using the `requestBody.jsonPath` property of a resource.

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

This example will match a request body like this:

```json
{ "foo": "bar" }
```

Any of the match operators, such as `Contains`, `Matches` etc. can be used in a JsonPath matcher.

#### Unmatched or null JsonPath expressions

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

### Matching an XML request body

You can match a resource based on a XPath query of a XML request body.

> Only XML request bodies are supported for the feature.

Specify the match configuration using the `requestBody.xPath` property of a resource.

Here you specify a XPath expression, relevant namespaces, and the value it must match.

For example:

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    xPath: "/env:Envelope/env:Body/pets:animal/pets:name"
    value: "Fluffy"
    xmlNamespaces:
      env: "http://schemas.xmlsoap.org/soap/envelope/"
      pets: "urn:com:example:petstore"
  response:
    statusCode: 204
```

This example will match a request body like this:

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

> Note: although this example uses a SOAP envelope, any valid XML body can be matched.

#### Reusing XML namespace definitions

Instead of specifying the namespace definitions for each XPath expression, you can define them once at the top level of the configuration:

```yaml
plugin: rest

resources:
- method: GET
  path: /example1
  requestBody:
    xPath: "/env:Envelope/env:Body/pets:animal/pets:name"
    value: "Fluffy"
  response:
    statusCode: 204

- method: GET
  path: /example2
  requestBody:
    xPath: "/env:Envelope/env:Body/pets:animal/pets:name"
    value: "Paws"
  response:
    statusCode: 400

# XML namespaces shared by all XPath expressions
system:
  xmlNamespaces:
    env: "http://schemas.xmlsoap.org/soap/envelope/"
    pets: "urn:com:example:petstore"
```

Any of the match operators, such as `Contains`, `Matches` etc. can be used in an XPath matcher.

#### Unmatched or null XPath expressions

If the result of evaluating the XPath expression is `null` or if the path evaluates to non-existent property in the body, then it is considered `null`.

You can explicitly match a `null` value, as follows:

```yaml
resources:
- method: GET
  path: /example-nonmatch
  requestBody:
    xPath: "/env:Envelope/env:Body/pets:animal/pets:nothing"
    # tilde is YAML for null
    value: ~
    xmlNamespaces:
      env: "http://schemas.xmlsoap.org/soap/envelope/"
      pets: "urn:com:example:petstore"
  response:
    statusCode: 409
```

> Note: the YAML keyword `null` indicates a null value, not the string literal `"null"`

### Matching raw request body content

You can match a resource based on the raw content of the request body.

Specify the match configuration using the `requestBody.operator` and `requestBody.value` properties of a resource.

Here you specify the operator and a value to compare.

For example:

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    operator: EqualTo
    value: bar
  response:
    statusCode: 204
```

This example will match a request body like this:

```
bar
```

Any of the match operators, such as `Contains`, `Matches` etc. can be used in a request body matcher.

#### Matching an empty request body

If the request body is empty, then it is considered `null`.

You can explicitly match a `null` value, as follows:

```yaml
resources:
- method: GET
  path: /example2
  requestBody:
    operator: EqualTo
    value: null
  response:
    statusCode: 409
```

> Note: the YAML keyword `null` indicates a null value, not the string literal `"null"`

---

### Using multiple request body matchers

You can use multiple request body matchers for a resource. Using the `allOf` or `anyOf` conditions controls how the matchers are combined.

#### All matchers must match

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    allOf:
    - jsonPath: $.foo
      value: bar
    - jsonPath: $.baz
      value: qux
  response:
    statusCode: 204
```

#### At least one matcher must match

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    anyOf:
    - jsonPath: $.foo
      value: bar
    - jsonPath: $.baz
      value: qux
  response:
    statusCode: 204
```

#### Body match operators

Body matchers support the range of operators described in this document.

For example:

```yaml
resources:
- method: GET
  path: /example1
  requestBody:
    jsonPath: $.foo
    value: bar
    operator: NotEqualTo
  response:
    statusCode: 400
```

> **Note**
> If no `operator` is specified, then `EqualTo` is used.

## Resource matching performance

[Resource matching](./configuration.md) is typically the fastest method of providing conditional responses. This is the case for request properties such as headers, query parameters, path parameters, path and HTTP method. In the case of using JsonPath or XPath to query the request body to conditionally match resources, however, the body must be parsed, which is computationally expensive and will result in lower performance.
