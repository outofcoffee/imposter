# Advanced request matching

You can match resources to requests using attributes such as the HTTP method, path, query string, headers or body (both JSON and XML supported).

## Matching on HTTP method, path, query string or headers

See [Configuration](./configuration.md) for details of matching on HTTP method, path, query string or headers.

## Matching on JSON body

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

## Matching on XML body

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

### Reusing XML namespace definitions

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

### Unmatched or null XPath expressions

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

## Using multiple request body matchers

You can use multiple request body matchers for a resource. Using the `allOf` or `anyOf` conditions controls how the matchers are combined.

### All matchers must match

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

### At least one matcher must match

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

## Resource matching performance

[Resource matching](./configuration.md) is typically the fastest method of providing conditional responses. This is the case for request properties such as headers, query parameters, path parameters, path and HTTP method. In the case of using JsonPath or XPath to query the request body to conditionally match resources, however, the body must be parsed, which is computationally expensive and will result in lower performance.
