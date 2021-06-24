# Stores (Experimental)

> **Stores are experimental**
>
> This means the API is subject to change or removal in future versions.

Imposter allows you to store data for use later. This can be useful for:

- use data from one request in the response to a later request
- tests to verify what was sent to a mock
- test data setup before a mock is used

## Enabling Stores

To enable stores, set the following environment variable:

    IMPOSTER_EXPERIMENTAL="stores"

The following log entry will be printed on startup:

```
WARN  i.g.i.s.s.StoreServiceImpl - Experimental store support enabled
```

You will now have access to a new object in [scripts](./scripting.md), named `stores`, and the `/system/store` API will be available. 

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

## The Stores API

You can also retrieve or set store data through the `/system/store` API. This can be useful for tests to verify what was sent to a mock:

```shell
$ curl http://localhost:8080/system/store/test/foo
ada
```

> In this example, imagine the item `foo` in the `test` store was set to the value `ada` by a previous request or script (see above).

You can also set data via this API:

```shell
$ curl -XPUT --data 'ada' http://localhost:8080/system/store/test/foo
```

Your scripts can now use the data in the store - this can be useful for test data setup.

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

> This deletes the `test` store and all its data.
