# Response template queries

Imposter allows you to respond with a template - that is, a file or content containing placeholders, which are replaced with values at runtime. See the [templates](./templates.md) page for more information.

You can also use a JsonPath expression to query an object when using a placeholder.

This is useful if you have an object, such as a request body or object within a [store](./stores.md), and wish to render a child property of the object instead of the whole object.

## Using JsonPath in placeholders

The JsonPath placeholder syntax is as follows:

```
<placeholder expression>:<JsonPath expression>
```

Here is an example that queries the request body:

```
${context.request.body:$.name}
```

In this example, the request body is parsed as a JSON object, then the JsonPath expression `$.name` is executed. The request body looks like this:

```json
{
  "name": "Alice",
  "occupation": "Programmer"
}
```

The result of the query is the string `Alice`, which is then substituted into the template.

Similarly, you could refer to other properties of the body - `occupation` would look like this:

```
Your occupation is: ${context.request.body:$.occupation}
```

### Example using stores

This example fetches an item in a store, and returns the `name` property using a JsonPath expression.

> Learn more about [stores](./stores.md).

This will return the `name` property from the `person` item in the `request` store:

```
${stores.request.person:$.name}
```

This retrieves the item named `person` from the `request` store. The item must be an object, or a string representation of a valid JSON object.

The `person` item looks like this:

```json
{
  "name": "Alice",
  "occupation": "Programmer"
}
```

Since this item is an object, we can use JsonPath to query the `name` property - hence the expression `$.name` in the template placeholder.

## Templating performance

Using JsonPath in placeholder templates is computationally expensive, as it requires parsing and querying of an item rather than just value substitution.

## Examples

- [response-template](https://github.com/imposter-project/examples/blob/main/rest/response-template)
