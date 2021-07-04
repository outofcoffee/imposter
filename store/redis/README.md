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

> See [example Redisson configuration](./examples/redisson.yaml).
> Refer to Redisson documentation for full details.

## Expiration

Set an expiration (default 1,800 seconds) for store items using the environment variable:

    IMPOSTER_STORE_REDIS_EXPIRY=120

> This sets item expiration to 120 seconds.

A negative value means no expiry.
