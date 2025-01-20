# Directory-based responses

## Overview

The directory-based response feature allows you to serve files from a directory based on the incoming request path. This is useful when you want to serve multiple files without having to configure each response individually.

## Configuration

The feature is configured using two key elements:
1. A trailing wildcard (`*`) in the request path matcher
2. A `dir` property in the response configuration that points to the directory containing response files

## Behavior

- When a request matches a path pattern with a wildcard, the system:
    1. Takes the part of the actual request path that matches after the non-wildcard portion
    2. Looks for a file with that name in the specified directory
    3. Serves that file as the response
- Content-Type is automatically inferred from the file extension
- If no matching file is found, returns a 500 error
- If the path pattern doesn't contain a wildcard but `dir` is specified, Imposter returns an HTTP 500 error
- If the request path ends with a `/`, Imposter looks for a file named `index.html` in the directory

## Requirements

- The request matcher path MUST end with a wildcard (`*`)
- The `dir` property MUST be specified in the response configuration
- The directory path is relative to the config file location

## Example Configuration

Assuming the following directory structure:

```
imposter-config.yaml
responses/
  example1.json
  data/
    example3.json
```

Here's an example configuration:

```yaml
# imposter-config.yaml
---
resources:
- request:
  method: GET
  # Wildcard path pattern
  path: /api/responses/*
  response:
    # Directory containing response files
    dir: responses # Directory containing response files
```

This would match:
```
GET /api/responses/example1.json -> serves responses/example1.json
GET /api/responses/data/example3.json -> serves responses/data/example3.json
```
