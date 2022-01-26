# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks. You load one or more plugins.

The following sections describe the available and built-in plugins. You can also write your own, if you want to customise behaviour further.

| Category        | Plugin name             | Description                            | Details                                                                            |
|-----------------|-------------------------|----------------------------------------|------------------------------------------------------------------------------------|
| Mock            | `hbase`                 | HBase mocks.                           | [HBase plugin](hbase_plugin.md)                                                    |
| Mock            | `openapi`               | OpenAPI (and Swagger) mocks.           | [OpenAPI (and Swagger) plugin](openapi_plugin.md)                                  |
| Mock            | `rest`                  | REST mocks.                            | [REST plugin](rest_plugin.md)                                                      |
| Mock            | `sfdc`                  | SFDC (Salesforce) mocks.               | [SFDC (Salesforce) plugin](sfdc_plugin.md)                                         |
| Scripting       | `js-graal`              | Graal.js scripting.                    | Graal.js JavaScript scripting support.                                             |
| Scripting       | `js-nashorn-standalone` | Nashorn standalone scripting.          | Nashorn JavaScript scripting support for Java 11+.                                 |
| Store           | `redis`                 | DynamoDB store implementation.         | [DynamoDB store](https://github.com/outofcoffee/imposter/tree/main/store/dynamodb) |
| Store           | `dynamodb`              | Redis store implementation.            | [Redis store](https://github.com/outofcoffee/imposter/tree/main/store/redis)       |
| Configuration   | `config-detector`       | Detects plugins from `META-INF`.       | Built-in.                                                                          |
| Configuration   | `meta-detector`         | Detects plugins from `*-config` files. | Built-in.                                                                          |

## The plugin directory

Imposter loads plugins from the _plugin directory_. This is configured using the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

When you set this environment variable, plugin JAR files placed in this directory will be loaded by Imposter on startup.

## Using the CLI

If you are using the [Imposter CLI](./run_imposter_cli.md), you can install a plugin with:

    imposter plugin install <plugin name>

The CLI automatically manages the plugin directory, so you do not have to set the `IMPOSTER_PLUGIN_DIR` environment variable.

For example:

    imposter plugin install stores-dynamodb

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.
