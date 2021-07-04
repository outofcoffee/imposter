# Redis store

Redis store implementation. Ensure [Stores](../../docs/stores.md) are enabled, then enable this module with the environment variable:

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

> See [example Redisson configuration](example/redisson.yaml).
> Refer to Redisson documentation for full details.

## Example

Run it:

    export IMPOSTER_EXPERIMENTAL=stores
    export IMPOSTER_STORE_MODULE=io.gatehill.imposter.store.redis.RedisStoreModule
    docker run --rm -ti -p 8080:8080 -v $PWD/store/redis/example:/opt/imposter/config outofcoffee/imposter

Test it:

    $ curl -XPUT http://localhost:8080/store?foo=bar
    $ curl http://localhost:8080/load
    bar

## Expiration

Set an expiration (default 1,800 seconds) for store items using the environment variable:

    IMPOSTER_STORE_REDIS_EXPIRY=120

> This sets item expiration to 120 seconds.

The unit is seconds. A negative value means no expiry.
