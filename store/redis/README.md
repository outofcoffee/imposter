# Redis store

Redis store implementation. Ensure [Stores](../../docs/stores.md) are enabled, then activate this module with the environment variable:

    IMPOSTER_STORE_MODULE="io.gatehill.imposter.store.redis.RedisStoreModule" 

Add a `redisson.yaml` file to your Imposter configuration directory, e.g.

```yaml
singleServerConfig:
  address: "redis://127.0.0.1:6379"
  idleConnectionTimeout: 10000
  connectTimeout: 10000
  timeout: 3000
  retryAttempts: 3
  retryInterval: 1500
```

> See an [example Redisson configuration](example/redisson.yaml) file.
>
> For full details refer to the [official Redisson documentation](https://github.com/redisson/redisson/wiki/2.-Configuration).

## Example

Start Redis:

    docker run --rm -it -p6379:6379 redis:5-alpine

> There should now be a running Redis instance on your local machine, on port 6379.

Start Imposter:

    docker run --rm -ti -p 8080:8080 \
        -e IMPOSTER_STORE_MODULE=io.gatehill.imposter.store.redis.RedisStoreModule \
        -v $PWD/store/redis/example:/opt/imposter/config \
        outofcoffee/imposter

Test writing a value:

    $ curl -XPUT http://localhost:8080/store?foo=bar

Test retrieving the value:

    $ curl http://localhost:8080/load
    bar

Note, the `store` and `load` endpoints above are part of the configuration for this example. See the [Stores](../../docs/stores.md) documentation for full API details.

## Expiration

Items can be set to expire from the store after a period of time. The default expiry is 1,800 seconds.

You can set the expiration using the following environment variable. A negative value means no expiry. The time unit is seconds.

    IMPOSTER_STORE_REDIS_EXPIRY=120

> This sets item expiration to 120 seconds.
