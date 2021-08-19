# Response templates

Imposter allows you to respond with a template - that is, a file containing placeholders, which are replaced with values at runtime.

Templates can be useful when you are [capturing data](./data_capture.md), using [stores](./stores.md) or generating data from [a script](./scripting.md).

Templates can be used with configuration or scripts.

## Configuration-driven templates

When you are using [configuration](./configuration.md) files to control mock behaviour, you can use the `template: true` property of the response object, as follows:

```yaml
resources:
  - path: /example
    method: GET
    response:
      staticFile: example-template.json
      template: true
```

In this example, the template file ('example-template.txt') might look like this:

```
{
  "userName": "${testStore.user}"
}
```

Notice the placeholder: `${testStore.user}` - this refers to an item named 'user' in the store named 'testStore'.

> Learn more about [stores](./stores.md).

A common pattern is to [capture](./data_capture.md) elements of the request into a store and use them in a templated response.

## Script-driven templates

When you are using [scripting](./scripting.md) to control mock behaviour, you can use the `template()` method, as follows:

```js
respond().withFile('example-template.json').template();
```

As with the configuration-driven approach described above, your template includes placeholders referring to data items in a store.

> Learn more about [stores](./stores.md).

A common pattern would be to retrieve items from a store using a script, or generate values dynamically and set them in the `request` store, for use by a template.

## Using JsonPath in placeholders

You can use a JsonPath expression to query a complex object in a placeholder.

This is useful if you have stored/captured an object, such as from a request body, and wish to use some part of the object instead of the whole object in a template.

The syntax is as follows:

```
${STORE_NAME.ITEM_NAME:JSONPATH_EXPRESSION}
```

For example:

```
${request.person:$.name}
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
Your occupation is: ${request.person:$.occupation}
```

## Templating performance

Templating incurs a performance penalty, but is often faster than dynamically generating large objects using scripts, so is generally a better tradeoff when dynamic responses are required.

Template files are cached in memory once read from disk, so they do not incur as high an I/O cost from storage on subsequent requests.

Using JsonPath in placeholder templates is computationally expensive, as it requires parsing and querying of an item rather than just value substitution.
