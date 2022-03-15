# Roadmap

This section lists future ideas for features and improvements. Feel free to submit a suggestion by raising an issue.

## Features

* Non-HTTP transports
* Asynchronous requests (i.e. callbacks)
* Asynchronous responses
* Scheduled HTTP(S) invocations
* Scheduled script executions
* Request and response validation against a JSON Schema file (instead of just OpenAPI spec)
* SOAP plugin - validate request/response body against XSD.
* Autogenerate OpenAPI spec/UI for REST plugin.

## Improvements

### Capture

- Replace `const` and `expression` with `value` key in capture block.

### HBase

* Add content type header to HBase response
* Reuse HBase model classes for JSON serialisation

## Breaking changes

The following items are breaking changes, such as removal of deprecated functionality. They will be removed or changed in a backwards incompatible way in a future major version.

- Drop Java 8 support.
- Removal of legacy `context.params` map - use `context.request.queryParams` instead
- Removal of legacy `context.request.params` map - use `context.request.queryParams` instead
- Removal of legacy `context.uri` map - use `context.request.uri` instead
- Request header keys will be lowercased (set `IMPOSTER_NORMALISE_HEADER_KEYS=true`)
- Request and response validation will be enabled by default
- Use of Query instead of Scan operation in DynamoDB store plugin (set `IMPOSTER_DYNAMODB_SCAN_TO_LIST_ALL=false`)
