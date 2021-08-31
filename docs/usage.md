# Usage (command line arguments)

Some options can be controlled using command line arguments. These are described in this section.

## Command line arguments

The following command line arguments can be used:

     --configDir (-c) VAL   : Directory containing mock configuration files
     --help (-h)            : Display usage only
     --host (-b) VAL        : Bind host
     --keystorePassword VAL : Password for the keystore (default: password)
     --keystorePath VAL     : Path to the keystore (default: classpath:/keystore/ssl.jks)
     --listenPort (-l) N    : Listen port (default: 8080)
     --plugin (-p) VAL      : Plugin name (e.g. rest) or fully qualified class
     --pluginArg VAL        : A plugin argument (key=value)
     --serverUrl (-u) VAL   : Explicitly set the server address
     --tlsEnabled (-t)      : Whether TLS (HTTPS) is enabled (requires keystore to be configured) (default: false)
     --version (-v)         : Print version and exit

## Environment variables

The following environment variables are supported:

| Variable name                                 | Purpose                                                                                                                   | Default                                                | Description/example(s)                           |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|--------------------------------------------------|
| IMPOSTER_LOG_LEVEL                            | Sets logging level.                                                                                                       | `DEBUG`                                                | `INFO`, `DEBUG`, `TRACE`                         |
| IMPOSTER_FEATURES                             | Enables or disables features. See [Features](features_plugins.md) documentation.                                          | Per [default features](./features_plugins.md).         | `metrics=false,stores=true`                      |
| IMPOSTER_STORE_MODULE                         | Sets the store implementation.                                                                                            | `io.gatehill.imposter.store.inmem.InMemoryStoreModule` | See [Stores](./stores.md).                       |
| IMPOSTER_STORE_KEY_PREFIX                     | Sets a prefix for store keys.                                                                                             | Empty                                                  | See [Stores](./stores.md).                       |
| IMPOSTER_SCRIPT_CACHE_ENTRIES                 | The number of precompiled scripts to cache. Precompiled scripts execute faster, but the cache uses memory.                | `20`                                                   | `30`                                             |
| IMPOSTER_RESPONSE_FILE_CACHE_ENTRIES          | The number of response files to cache in memory. Cached response files don't require disk I/O, but the cache uses memory. | `20`                                                   | `30`                                             |
| IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR | The default behaviour for OpenAPI validation issues. See [OpenAPI validation](openapi_validation.md).                     | `IGNORE`                                               | See [OpenAPI validation](openapi_validation.md). |

> Note: other features may include their own environment variables. See the feature specific documentation for more details.

## Server URL

For some responses, such as from the [SFDC plugin](sfdc_plugin.md), Imposter uses the 'server URL', which is computed automatically from the `host` and `listenPort` command line arguments. If this is not the URL you wish to use, you can override this with the `serverUrl` command line argument.

## Security

See [Security](security.md).
