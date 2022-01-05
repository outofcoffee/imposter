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
