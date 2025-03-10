# Generating fake data

You can generate fake data for fields in your mock.

Examples include, but are not limited to:

* Names
* Email addresses
* Addresses

## Setup

### Option 1: Using the `fake-data` plugin

To use it, install the `fake-data` plugin:

```bash
imposter plugin install -d fake-data
```

### Option 2: Using the 'all' distribution

Alternatively, you can use the `all` distribution, which includes the `fake-data` plugin.

This is the `docker-all` engine type if you're using [the CLI](./run_imposter_cli.md) or the `imposter-all.jar` file if you're using the [JAR file](./run_imposter_jar.md) approach.

## Generating fake data

### Fake data expressions

You can use [template expressions](./templates.md) in responses to insert fake data. The expressions start with `fake.` followed by a type of fake data to generate.

For example, to return a random first name, use the following expression:

    ${fake.Name.firstName}

For an email address:

    ${fake.Internet.emailAddress}

Some common examples:

| Fake data      | Expression                     |
|----------------|--------------------------------|
| First name     | `fake.Name.firstName`          |
| Last name      | `fake.Name.lastName`           |
| Email address  | `fake.Internet.emailAddress`   |
| Username       | `fake.Name.username`           |
| Street address | `fake.Address.streetAddress`   |
| City           | `fake.Address.city`            |
| Country        | `fake.Address.country`         |
| Phone number   | `fake.PhoneNumber.phoneNumber` |

> **Note**
> Valid values are those supported by the [Datafaker](https://github.com/datafaker-net/datafaker) library.

#### Try it out

See the [example directory](https://github.com/imposter-project/imposter-jvm-engine/blob/main/examples/rest/fake-data) for a working example.

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

When using the `fake-data` plugin with the OpenAPI plugin, common property names in the OpenAPI specification, such as `firstName`, `email` etc., are replaced with fake data. This happens when no matching example is found in the specification.

#### Try it out

See the [example directory](https://github.com/imposter-project/imposter-jvm-engine/blob/main/examples/openapi/fake-data) for a working example.

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
