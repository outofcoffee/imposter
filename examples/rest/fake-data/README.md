# Returning fake data

This example shows how to generate fake data for fields in your mock.

To use it, install the `fake-data` plugin:

```bash
imposter plugin install fake-data
```

In the response, placeholders are replaced with fake data.

For example:

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

Valid values are those supported by the [Datafaker](https://github.com/datafaker-net/datafaker) library.
