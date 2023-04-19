# Usage (command line arguments and environment variables)

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

| Variable name                                 | Purpose                                                                                                                        | Default                                     | Description/example(s)                                                                          |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------|
| IMPOSTER_ADD_ENGINE_RESPONSE_HEADERS          | Whether to add response headers for `server` and unique request ID.                                                            | `true`                                      | `false`                                                                                         |
| IMPOSTER_CACHE_DIR                            | Path to a directory in which to store cached data, such as remote specifications.                                              | `<Java temporary directory>/imposter-cache` | `/path/to/dir`                                                                                  |
| IMPOSTER_CONFIG_SCAN_RECURSIVE                | Whether to scan for configuration files recursively within the configuration directories.                                      | `false`                                     | `true`                                                                                          |
| IMPOSTER_CONFIG_DISCOVER_ENVFILES             | Whether to discover envfiles. See [Usage](./usage.md).                                                                         | `true`                                      | `false`                                                                                         |
| IMPOSTER_FEATURES                             | Enables or disables features. See [Features](./features.md) documentation.                                                     | Per [default features](./features.md).      | `metrics=false,stores=true`                                                                     |
| IMPOSTER_JS_PLUGIN                            | Sets the JavaScript implementation.                                                                                            | `js-nashorn-standalone`                     | Can be changed to `js-graal` plugin, if installed.                                              |
| IMPOSTER_LOG_LEVEL                            | Sets logging level.                                                                                                            | `DEBUG`                                     | `INFO`, `DEBUG`, `TRACE`                                                                        |
| IMPOSTER_LOG_REQUEST_BODY                     | Whether include request in structured log entries.                                                                             | `false`                                     | `true` - See [Metrics, logs and telemetry](./metrics_logs_telemetry.md).                        |
| IMPOSTER_LOG_REQUEST_HEADERS                  | Comma separated list of request headers to add to structured log entries.                                                      | Empty                                       | `X-Correlation-ID,User-Agent` - See [Metrics, logs and telemetry](./metrics_logs_telemetry.md). |
| IMPOSTER_LOG_RESPONSE_BODY                    | Whether include response in structured log entries.                                                                            | `false`                                     | `true` - See [Metrics, logs and telemetry](./metrics_logs_telemetry.md).                        |
| IMPOSTER_LOG_RESPONSE_HEADERS                 | Comma separated list of response headers to add to structured log entries.                                                     | Empty                                       | `Server,Content-Type` - See [Metrics, logs and telemetry](./metrics_logs_telemetry.md).         |
| IMPOSTER_LOG_SUMMARY                          | Whether to log a JSON formatted summary message on each request.                                                               | `false`                                     | `true`                                                                                          |
| IMPOSTER_LOG_SUMMARY_PRETTY                   | Whether to pretty print the summary log JSON.                                                                                  | `false`                                     | `true`                                                                                          |
| IMPOSTER_NORMALISE_HEADER_KEYS                | Forces header keys to be lowercased.                                                                                           | `true`                                      | boolean                                                                                         |
| IMPOSTER_OPENAPI_EXPOSE_SPEC                  | Whether to expose the OpenAPI specification and UI. See [OpenAPI plugin](openapi_plugin.md).                                   | `true`                                      | `false`                                                                                         |
| IMPOSTER_OPENAPI_REMOTE_FILE_CACHE            | Whether to locally cache remote OpenAPI specifications. See [OpenAPI plugin](openapi_plugin.md).                               | `false`                                     | `true`                                                                                          |
| IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR | The default behaviour for OpenAPI validation issues. See [OpenAPI validation](openapi_validation.md).                          | `IGNORE`                                    | See [OpenAPI validation](openapi_validation.md).                                                |
| IMPOSTER_PLUGIN_ARGS                          | Comma separated map of plugin arguments. Analogous to `--pluginArg` command line argument.                                     | Empty                                       | `openapi.scheme=https,openapi.title=Mocks`                                                      |
| IMPOSTER_PLUGIN_CLASSLOADER_STRATEGY          | Sets the classloader strategy for plugins.                                                                                     | Parent-first                                | `child`                                                                                         |
| IMPOSTER_PLUGIN_DIR                           | Path to a directory containing additional plugin JAR files.                                                                    | Empty                                       | Used by [Stores](./stores.md) and [Plugins](./plugins.md).                                      |
| IMPOSTER_PREFER_EXACT_MATCH_ROUTES            | Whether to prefer routes with exact matches over those with path placeholders when matching requests.                          | `true`                                      | boolean                                                                                         |
| IMPOSTER_STORE_DRIVER                         | Sets the store driver plugin.                                                                                                  | `store-inmem`                               | See [Stores](./stores.md).                                                                      |
| IMPOSTER_STORE_KEY_PREFIX                     | Sets a prefix for store keys.                                                                                                  | Empty                                       | See [Stores](./stores.md).                                                                      |
| IMPOSTER_SCRIPT_CACHE_ENTRIES                 | The number of precompiled scripts to cache. Precompiled scripts execute faster, but the cache uses memory.                     | `20`                                        | `30`                                                                                            |
| IMPOSTER_SCRIPT_PRECOMPILE                    | Whether to precompile scripts at startup.                                                                                      | `true`                                      | boolean                                                                                         |
| IMPOSTER_RESPONSE_FILE_CACHE_ENTRIES          | The number of response files to cache in memory. Cached response files don't require disk I/O, but the cache uses memory.      | `20`                                        | `30`                                                                                            |
| IMPOSTER_YAML_CODE_POINT_LIMIT                | The maximum number of characters to parse in a YAML file. The default is 3MB. Note that the size depends on the file encoding. | `3145728` (3MB)                             | `10485760` (10MB)                                                                               |

> Note: other features may include their own environment variables. See the feature specific documentation for more details.

## Setting environment variables using a file

You can use an environment file ('envfile') to pass environment variables to Imposter. To do this, add a file named `.env` adjacent to your configuration files, for example:

```
$ ls
.env
imposter-config.yaml

$ cat .env
IMPOSTER_LOG_LEVEL=info
OTHER_ENV_VAR=example
```

## Server URL

For some responses, such as from the [SFDC plugin](sfdc_plugin.md), Imposter uses the 'server URL', which is computed automatically from the `host` and `listenPort` command line arguments. If this is not the URL you wish to use, you can override this with the `serverUrl` command line argument.

## Security configuration

See the [Security](security.md) section.
