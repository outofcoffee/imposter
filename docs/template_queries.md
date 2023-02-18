# Response templates queries

Imposter allows you to respond with a template - that is, a file containing placeholders, which are replaced with values at runtime.

> See the [Templates](./templates.md) page for more information on templates.

## Using JsonPath in placeholders

You can use a JsonPath expression to query a complex object in a placeholder.

This is useful if you have stored/captured an object, such as from a request body, and wish to use some part of the object instead of the whole object in a template.

The syntax is as follows:

```
stores.<store name>.<item name>:<JsonPath expression>
```

For example:

```
${stores.request.person:$.name}
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
Your occupation is: ${stores.request.person:$.occupation}
```

## Templating performance

Using JsonPath in placeholder templates is computationally expensive, as it requires parsing and querying of an item rather than just value substitution.

## Examples

- [response-template](https://github.com/outofcoffee/imposter/blob/main/examples/rest/response-template)
