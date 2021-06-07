# Roadmap

This section lists future ideas for features and improvements. Feel free to submit a suggestion by raising an issue.

## Features

* Persistent storage for posted data, to enable it to be returned later (REST/OpenAPI plugin)
* Non-HTTP transports
* Asynchronous requests (i.e. callbacks)
* Asynchronous responses
* Request and response validation against a JSON Schema file (instead of just OpenAPI spec)
* Enabling request and response validation by default

## Improvements

### HBase

* Add content type header to HBase response
* Reuse HBase model classes for JSON serialisation

## Deprecated

The following features are deprecated and will be removed in a future major version.

- Legacy `context.params` map - use `context.request.queryParams` instead
- Legacy `context.request.params` map - use `context.request.queryParams` instead
- Legacy `context.uri` map - use `context.request.uri` instead
