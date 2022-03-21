# GraphQL store queries

When using [Stores](./stores.md), you can access and manipulate data using the Stores REST API, or using GraphQL.

> See the [Stores documentation](./stores.md) for information about its REST API.

Supported features:

- Query items by store
- Filter items by key prefix

## Install GraphQL plugin

Imposter provides GraphQL support using a plugin.

### Option 1: Install plugin using the CLI

To use this plugin, install it with the [Imposter CLI](./run_imposter_cli.md):

    imposter plugin install -d store-graphql

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Install plugin using the JAR

To use this plugin, download the plugin `imposter-plugin-store-graphql.jar` JAR file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Enable it with the following environment variables:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Execute a GraphQL query

The GraphQL query endpoint is `/system/store/graphql`, for example:

    http://localhost:8080/system/store/graphql

Here is a query to retrieve all items in a given store:

```
query {
  items(storeName: "test") {
    key
    value
  }
}
```

This yields the following result:

```json
{
  "data": {
    "items": [
      { "key":  "foo", "value":  "bar" },
      { "key":  "baz", "value":  "qux" }
    ]
  }
}
```

You can filter the items returned via a key prefix:

```
query {
  items(storeName: "test", keyPrefix: "f") {
    key
    value
  }
}
```

This yields a single result, with the key matching the prefix `"f"`:

```json
{
  "data": {
    "items": [
      { "key":  "foo", "value":  "bar" }
    ]
  }
}
```
