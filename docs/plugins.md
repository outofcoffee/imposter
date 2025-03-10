# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks.

> You can also write your own plugins, if you want to customise behaviour further.

The following table describes the available plugins.

| Category       | Plugin name       | Description                            | Details                                                                                                                                          |
|----------------|-------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Mock           | `hbase`           | HBase mocks.                           | See [HBase plugin](hbase_plugin.md).                                                                                                             |
| Mock           | `openapi`         | OpenAPI (and Swagger) mocks.           | Built-in. See [OpenAPI (and Swagger) plugin](openapi_plugin.md).                                                                                 |
| Mock           | `rest`            | REST mocks.                            | Built-in. See [REST plugin](rest_plugin.md).                                                                                                     |
| Mock           | `sfdc`            | SFDC (Salesforce) mocks.               | See [SFDC (Salesforce) plugin](sfdc_plugin.md).                                                                                                  |
| Mock           | `soap`            | SOAP (and WSDL) mocks.                 | Built-in. See [SOAP plugin](soap_plugin.md).                                                                                                     |
| Mock           | `wiremock`        | WireMock mappings support.             | See [WireMock plugin](wiremock_plugin.md).                                                                                                       |
| Scripting      | `js-graal`        | Graal.js scripting.                    | Graal.js JavaScript scripting support. This is the default JavaScript script engine. See [Modern JavaScript features](./scripting_modern_js.md). |
| Scripting      | `js-nashorn`      | Nashorn scripting.                     | This is the legacy JavaScript script engine                                                                                                      |
| Store          | `store-dynamodb`  | DynamoDB store implementation.         | See [DynamoDB store](https://github.com/imposter-project/imposter-jvm-engine/tree/main/store/dynamodb).                                                          |
| Store          | `store-redis`     | Redis store implementation.            | See [Redis store](https://github.com/imposter-project/imposter-jvm-engine/tree/main/store/redis).                                                                |
| Store          | `store-graphql`   | GraphQL store queries.                 | See [GraphQL](stores_graphql.md).                                                                                                                |
| Configuration  | `config-detector` | Detects plugins from `*-config` files. | Built-in                                                                                                                                         |
| Configuration  | `meta-detector`   | Detects plugins from `META-INF`.       | Built-in                                                                                                                                         |
| Data generator | `fake-data`       | Generates fake data.                   | See [Fake data generator](fake_data.md).                                                                                                         |

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

The Docker container sets the environment variable `IMPOSTER_PLUGIN_DIR=/opt/imposter/plugins`, so you do not have to set it explicitly.
