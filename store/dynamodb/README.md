# DynamoDB store

DynamoDB store implementation. Ensure [Stores](../../docs/stores.md) are enabled, then activate this module with the environment variable:

    IMPOSTER_STORE_MODULE="io.gatehill.imposter.store.dynamodb.DynamoDBStoreModule"

The following variables can be set:

| Environment variable             | Purpose                                     | Default                    |
|----------------------------------|---------------------------------------------|----------------------------|
| IMPOSTER_DYNAMODB_TABLE          | DynamoDB table name.                        | `"Imposter"`               |
| IMPOSTER_DYNAMODB_SIGNING_REGION | The signing region for the DynamoDB client. | Inferred from environment. |
