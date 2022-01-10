# Redis store

Redis store implementation. 

To use this plugin, download the plugin JAR file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Enable it with the following environment variables:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"
    IMPOSTER_STORE_DRIVER="store-redis"

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

> See the [example](https://raw.githubusercontent.com/outofcoffee/imposter/store/redis/example) directory for sample configuration files.

Start a local Redis instance:

    docker run --rm -it -p6379:6379 redis:5-alpine

> There should now be a running Redis instance on your local machine, on port 6379.

Place the plugin JAR file into a directory named `plugins`. Let's add a simple config file as well, under `config`:

### Simple example

```yaml
# imposter-config.yaml
plugin: rest
```

Your local filesystem should look like:

```
.
├── config
│  ├── imposter-config.yaml
│  └── redisson.yaml
└── plugins
   └── imposter-plugin-store-redis.jar
```

Start Imposter:

    docker run --rm -ti -p 8080:8080 \
        -e IMPOSTER_STORE_DRIVER=store-redis \
        -v $PWD/config:/opt/imposter/config \
        -v $PWD/plugins:/opt/imposter/plugins \
        outofcoffee/imposter

Test writing a value:

    $ curl -XPUT http://localhost:8080/system/store/test/foo -d 'bar'

Test retrieving the value:

    $ curl http://localhost:8080/system/store/test/foo
    bar

### Scripted example

Start Imposter as above, using the scripted example configuration files in the [example](https://raw.githubusercontent.com/outofcoffee/imposter/store/redis/example) directory.

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
