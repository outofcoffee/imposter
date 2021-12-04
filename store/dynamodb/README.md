# DynamoDB store

DynamoDB store implementation. Ensure [Stores](../../docs/stores.md) are enabled, then activate this module with the environment variable:

    IMPOSTER_STORE_MODULE="io.gatehill.imposter.store.dynamodb.DynamoDBStoreModule"

The following variables can be set:

| Environment variable             | Purpose                                     | Default                    |
|----------------------------------|---------------------------------------------|----------------------------|
| IMPOSTER_DYNAMODB_TABLE          | DynamoDB table name.                        | `"Imposter"`               |
| IMPOSTER_DYNAMODB_SIGNING_REGION | The signing region for the DynamoDB client. | Inferred from environment. |

## DynamoDB set up

Create a DynamoDB table with the following configuration:

* Partition key: `StoreName` (type String)
* Sort key: `Key` (type String)

By default, this table should be called `Imposter`. If you choose a different name, set the `IMPOSTER_DYNAMODB_TABLE` environment variable to match.
