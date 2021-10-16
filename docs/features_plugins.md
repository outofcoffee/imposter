# Features and plugins

## Features

Imposter allows certain features to be enabled or disabled. Fewer features lowers resource requirements.

### List of features

| Feature name    | Purpose                              | Details                                | Enabled by default |
|-----------------|--------------------------------------|----------------------------------------|--------------------|
| `metrics`       | Collects and exposes telemetry.      | [Metrics](./metrics_logs_telemetry.md) | `true`             |
| `stores`        | Persistent or semi-persistent state. | [Stores](./stores.md)                  | `true`             |

These can be controlled by setting the environment variable `IMPOSTER_FEATURES`:

    IMPOSTER_FEATURES="stores=false,metrics=true"

...or Java system property `imposter.features`:

    -Dimposter.features="stores=false,metrics=true"

## Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks. You load one or more plugins.

The following sections describe the built-in plugins. You can also write your own, if you want to customise behaviour further.

| Plugin name       | Description                            | Details                                           |
|-------------------|----------------------------------------|---------------------------------------------------|
| `config-detector` | Detects plugins from `META-INF`.       | Built-in.                                         |
| `hbase`           | HBase plugin.                          | [HBase plugin](hbase_plugin.md)                   |
| `meta-detector`   | Detects plugins from `*-config` files. | Built-in.                                         |
| `openapi`         | OpenAPI (and Swagger) plugin.          | [OpenAPI (and Swagger) plugin](openapi_plugin.md) |
| `rest`            | REST plugin.                           | [REST plugin](rest_plugin.md)                     |
| `sfdc`            | SFDC (Salesforce) plugin.              | [SFDC (Salesforce) plugin](sfdc_plugin.md)        |
