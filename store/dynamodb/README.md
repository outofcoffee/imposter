# DynamoDB store

DynamoDB store implementation.

## Install plugin

### Option 1: Using the CLI

To use this plugin, install it with the [Imposter CLI](../../docs/run_imposter_cli.md):

    imposter plugin install -d store-dynamodb

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Install the plugin manually

To use this plugin, download the `imposter-plugin-store-dynamodb.jar` JAR file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Set the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Configuration

To activate the plugin set the following environment variable:

    IMPOSTER_STORE_DRIVER="store-dynamodb"

The following variables can be set:

| Environment variable                    | Purpose                                                                                                                   | Default                    |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|----------------------------|
| IMPOSTER_DYNAMODB_OBJECT_SERIALISATION  | How to serialise objects; allowed values are `BINARY` or `MAP`.                                                           | `BINARY`                   |
| IMPOSTER_DYNAMODB_REGION                | The region for the DynamoDB client.                                                                                       | Inferred from environment. |
| IMPOSTER_DYNAMODB_TABLE                 | DynamoDB table name.                                                                                                      | `"Imposter"`               |
| IMPOSTER_DYNAMODB_TTL                   | The number of seconds to use for item TTL.                                                                                | No TTL set.                |
| IMPOSTER_DYNAMODB_TTL_ATTRIBUTE         | The name of TTL attribute in the table.                                                                                   | `ttl`                      |

## DynamoDB set up

Create a DynamoDB table with the following configuration:

* Partition key: `StoreName` (type String)
* Sort key: `Key` (type String)

By default, this table should be called `Imposter`. If you choose a different name, set the `IMPOSTER_DYNAMODB_TABLE` environment variable to match.

## Time to Live (TTL)

DynamoDB supports setting a TTL so items are removed after a period of time.

To use this, enable TTL for your table per the [AWS documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html).

Once you have enabled TTL (and allowed time for it to apply to your table), set the following environment variables:

    IMPOSTER_DYNAMODB_TTL=<number of seconds>
    IMPOSTER_DYNAMODB_TTL_ATTRIBUTE=<name of TTL attribute in table>

> By default, `IMPOSTER_DYNAMODB_TTL_ATTRIBUTE` is set to `ttl`.

Example:

    # five minutes
    IMPOSTER_DYNAMODB_TTL="300"
    
    # name of TTL attribute
    IMPOSTER_DYNAMODB_TTL_ATTRIBUTE="ttl"
