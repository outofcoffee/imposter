# Generating fake data

You can generate fake data for fields in your mock.

Examples include, but are not limited to:

* Names
* Email addresses
* Addresses

## Returning fake data

### Option 1: Using the `fake-data` plugin

To use it, install the `fake-data` plugin:

```bash
imposter plugin install fake-data
```

### Option 2: Using the 'all' distribution

Alternatively, you can use the `all` distribution, which includes the `fake-data` plugin.

This is the `docker-all` engine type if you're using the CLI or the `imposter-all.jar` file if you're using the JAR file approach.

## Using the `fake-data` plugin

### Fake data expressions

In the response, placeholders are replaced with fake data.

For example, to return a random first name, use the following expression:

    ${fake.Name.firstName}

For an email address:

    ${fake.Internet.email}

> **Note**
> Valid values are those supported by the [Datafaker](https://github.com/datafaker-net/datafaker) library.

#### Try it out

See the [example directory](../examples/rest/fake-data) for a working example.

```bash
curl http://localhost:8080/users/1
```

```json
{
  "id": 1,
  "firstName": "Linda",
  "lastName": "Smith",
  "email": "linda@example.com"
}
```

### Fake data in OpenAPI specs

> **Note**
> This section applies to the [OpenAPI plugin](openapi_plugin.md).

In an OpenAPI file, common property names, such as `firstName`, `email` etc. are replaced with fake data.

#### Try it out

See the [example directory](../examples/openapi/fake-data) for a working example.

For example:

```bash
curl http://localhost:8080/users/1
```

```json
{
  "id": 1,
  "firstName": "Linda",
  "lastName": "Smith",
  "email": "linda@example.com",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "postcode": "10001"
  },
  "favouriteColour": "red"
}
```

### Customizing the fake data

You can customize the fake data by adding a `x-fake-data` property to the field in the OpenAPI file.

This is done for the `favouriteColour` field in the example:

```yaml
favouriteColour:
  type: string
  x-fake-data: Color.name
```

> **Note**
> Valid values are those supported by the [Datafaker](https://github.com/datafaker-net/datafaker) library.
