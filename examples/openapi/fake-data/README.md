# Returning fake data

This example shows how to generate fake data for fields in your mock.

To use it, install the `fake-data` plugin:

```bash
imposter plugin install fake-data
```

In the OpenAPI file, common field names, such as `firstName`, `email` etc. are replaced with fake data.

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

Valid values are those supported by the [Datafaker](https://github.com/datafaker-net/datafaker) library.
