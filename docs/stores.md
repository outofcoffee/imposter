# Stores

Imposter allows you to store data for use later. Benefits:

- use data from one request in a later request
- your tests can verify what was sent to a mock by your application
- set up/seed test data before a mock is used

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
  .withData(previousFoo);
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

## The Stores API

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

| Variable name             | Purpose                                                              | Example(s)                                                                |
|---------------------------|----------------------------------------------------------------------|---------------------------------------------------------------------------|
| IMPOSTER_STORE_MODULE     | Sets the store implementation (see _Store implementations_ section). | `io.gatehill.imposter.store.redis.RedisStoreModule`                       |
| IMPOSTER_STORE_KEY_PREFIX | Sets an optional prefix for all keys in the store, like a namespace. | A prefix of `foo` would result in the key `bar` being stored as `foo.bar` |

## Store implementations

Different store implementations exist:

* In memory store (default)
* [Redis store](../store/redis)
