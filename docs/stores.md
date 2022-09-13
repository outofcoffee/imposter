# Stores

Imposter allows you to store data for use later. Benefits:

- use data from a request in a current or future response
- store the data sent to a mock for later retrieval/verification
- set up or seed test data before a mock is used
- [capture](./data_capture.md) data (headers, body etc.) for use by a script
- return stored data in a [response template](./templates.md)
- support for [GraphQL](./stores_graphql.md) queries

## Summary

You can access Stores via an object in [scripts](./scripting.md), named `stores`, and the `/system/store` REST API. 

## Using stores in scripts

Here is an example of using the `stores` object in your scripts.

```js
var exampleStore = stores.open('example');

// save the value of the 'foo' query parameter to the store
exampleStore.save('foo', context.request.queryParams.foo);

// ...in some other request, later
var previousFoo = exampleStore.load('foo');
respond()
  .withStatusCode(200)
  .withContent(previousFoo);
```

Stores also support deletion:

```js
exampleStore.delete('example');
```

and retrieving all data:

```js
var storeData = exampleStore.loadAll();
logger.info('foo=' + storeData.foo);
logger.info('bar=' + storeData.bar);
```

You can check for the presence of an item by key:

```js
if (exampleStore.hasItemWithKey('example')) {
    // ... there is an item with the key 'example'
}
```

## The Stores REST API

You can also retrieve or save data to a store through the `/system/store` API. This can be useful for tests to verify what was sent to a mock:

```shell
$ curl http://localhost:8080/system/store/test/foo
ada
```

> In this example, imagine the item `foo` in the `test` store was set to the value `ada` by a previous request or script (see above).

You can also set data via this API:

```shell
$ curl -XPUT --data 'ada' http://localhost:8080/system/store/test/foo
```

Your scripts can now use the data in the store. This can be useful for test data setup/seeding.

You can retrieve all the items in a store:

```shell
$ curl http://localhost:8080/system/store/test
{
  "foo": "ada",
  "bar": "baz"
}
```

> Note, you can filter the items returned by their key's prefix, such as:
>
> ```shell
> $ curl http://localhost:8080/system/store/test?keyPrefix=fo
> {
>   "foo": "ada"
> }
> ```

You can delete items in a store:

```shell
$ curl -XDELETE http://localhost:8080/system/store/test/foo
```

> This deletes the `foo` item from the `test` store.

You can delete an entire store and all of its data:

```shell
$ curl -XDELETE http://localhost:8080/system/store/test
```

> This deletes the whole `test` store and all its data.

You can set multiple items at once via a `POST` to the store resource:

```shell
$ curl -XPOST http://localhost:8080/system/store/test --data '{ "foo": "bar", "baz": "qux" }'
```

## Environment variables

The following environment variables are supported:

| Variable name             | Purpose                                                              | Default       | Example(s)                                                                |
|---------------------------|----------------------------------------------------------------------|---------------|---------------------------------------------------------------------------|
| IMPOSTER_STORE_DRIVER     | Sets the store implementation (see _Store implementations_ section). | `store-inmem` | `store-dynamodb`                                                          |
| IMPOSTER_STORE_KEY_PREFIX | Sets an optional prefix for all keys in the store, like a namespace. | Empty         | A prefix of `foo` would result in the key `bar` being stored as `foo.bar` |

## Request scoped store

There is a special request-scoped store, named `request`, which is accessible only to the current request. Its contents do not persist beyond the lifecycle of the request.

The request scoped store is very useful when you need to [capture](./data_capture.md) an item for immediate use, such as in a [response template](./templates.md), but you don't need to persist it for later use.

## Preloading (pre-populating) data into a store

You can preload data into a store when Imposter starts.

To do this, use the `system.stores.preloadFile` or `system.stores.preloadData` key in a configuration file.

### Preloading from file

Typically, you will provide the data in a JSON file, and use the `preloadFile` key:

```yaml
plugin: rest

system:
  stores:
    # this store is preloaded from file
    example:
      preloadFile: initial-data.json
```

In the above example, the contents of the file `initial-data.json` will be loaded into the store named 'example'.

This file contains a JSON object, such as the following:

```json
{
  "foo": "bar",
  "baz": {
    "qux": "corge"
  }
}
```

> Note that you can store child objects, but the top level keys must always be a string.

### Preloading from inline data

If you have a small amount of data, or you don't want to use a separate file, you can provide the preload data inline within a configuration file using the `preloadData` key:

```yaml
plugin: rest

system:
  stores:
    # this store is preloaded from inline data
    example:
      preloadData:
        foo: bar
        baz: { "qux": "corge" }
```

In the above example, the items under `preloadData` block will be loaded into the store named 'example'.

You must provide an object with key/value pairs, such as that shown above, or in the JSON file below. Top level keys must always be a string.

## Store implementations

Different store implementations exist:

- In memory store (default)
- [DynamoDB store](https://github.com/outofcoffee/imposter/tree/main/store/dynamodb)
- [Redis store](https://github.com/outofcoffee/imposter/tree/main/store/redis)

## GraphQL support

As well as the stores REST API described in this document, you can access and manipulate data [using GraphQL](./stores_graphql.md).
