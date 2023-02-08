# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks.

> You can also write your own plugins, if you want to customise behaviour further.

The following sections describe the available and built-in plugins.

| Category      | Plugin name             | Description                            | Details                                                                            |
|---------------|-------------------------|----------------------------------------|------------------------------------------------------------------------------------|
| Mock          | `hbase`                 | HBase mocks.                           | [HBase plugin](hbase_plugin.md)                                                    |
| Mock          | `openapi`               | OpenAPI (and Swagger) mocks.           | Built-in - see [OpenAPI (and Swagger) plugin](openapi_plugin.md)                   |
| Mock          | `rest`                  | REST mocks.                            | Built-in - see [REST plugin](rest_plugin.md)                                       |
| Mock          | `sfdc`                  | SFDC (Salesforce) mocks.               | [SFDC (Salesforce) plugin](sfdc_plugin.md)                                         |
| Mock          | `soap`                  | SOAP (and WSDL) mocks.                 | Built-in - see [SOAP plugin](soap_plugin.md)                                       |
| Mock          | `wiremock`              | WireMock mappings support.             | [WireMock plugin](wiremock_plugin.md)                                              |
| Scripting     | `js-graal`              | Graal.js scripting.                    | Graal.js JavaScript scripting support                                              |
| Scripting     | `js-nashorn-standalone` | Nashorn standalone scripting.          | Nashorn JavaScript scripting support for Java 11+                                  |
| Store         | `store-dynamodb`        | DynamoDB store implementation.         | [DynamoDB store](https://github.com/outofcoffee/imposter/tree/main/store/dynamodb) |
| Store         | `store-redis`           | Redis store implementation.            | [Redis store](https://github.com/outofcoffee/imposter/tree/main/store/redis)       |
| Store         | `store-graphql`         | GraphQL store queries.                 | [GraphQL](stores_graphql.md)                                                       |
| Configuration | `config-detector`       | Detects plugins from `META-INF`.       | Built-in                                                                           |
| Configuration | `meta-detector`         | Detects plugins from `*-config` files. | Built-in                                                                           |

## Plugin loading

Imposter loads plugins from the _plugin directory_. This is configured using the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

When you set this environment variable, plugin JAR files placed in this directory will be loaded by Imposter on startup.

## Using the CLI

If you are using the [Imposter CLI](./run_imposter_cli.md), you can install a plugin with:

    imposter plugin install -d <plugin name>

The CLI automatically manages the plugin directory, so you do not have to set the `IMPOSTER_PLUGIN_DIR` environment variable.

For example:

    imposter plugin install -d stores-dynamodb

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

## Using the Docker image

If you are using the [Docker image](./run_imposter_docker.md), you can bind-mount a local directory to the `/opt/imposter/plugins` directory within the container.

For example:

    docker run --rm -it \
        -v /path/to/plugin/dir:/opt/imposter/plugins \
        -v /path/to/config/dir:/opt/imposter/config \
        -p 8080:8080 \
        outofcoffee/imposter

The Docker container sets the environment variable `IMPOSTER_PLUGIN_DIR=/opt/imposter/config`, so you do not have to set it explicitly.
