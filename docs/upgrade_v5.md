# Upgrading to Imposter 5

This guide covers upgrading from Imposter 4 (and earlier) to Imposter 5.

## Overview

Imposter 5 is a ground-up rewrite in Go, replacing the JVM-based engine used in versions 1-4. This brings significant improvements to startup time, memory usage and binary size, but requires some changes to your configuration files and scripts.

> The JVM-based engine (versions 1-4) is available at [imposter-jvm-engine](https://github.com/imposter-project/imposter-jvm-engine).

## Scripting: JavaScript only

Imposter 5 supports **JavaScript** scripts only. If you have Groovy scripts, you must migrate them to JavaScript before upgrading.

Groovy and JavaScript scripts in Imposter share the same [scripting API](./scripting.md), so migration is primarily a syntax change rather than a logic change.

For example, a Groovy script like:

```groovy
def name = context.request.queryParams.name
if (name == 'test') {
    respond().withStatusCode(200).withContent('Hello')
} else {
    respond().withStatusCode(400)
}
```

...becomes the following JavaScript:

```javascript
var name = context.request.queryParams.name;
if (name === 'test') {
    respond().withStatusCode(200).withContent('Hello');
} else {
    respond().withStatusCode(400);
}
```

> See the [JavaScript scripting tips](./javascript_tips.md) page for more information.

## Configuration file changes

Imposter 5 uses an updated configuration format. The following changes are required.

### 1. Move root-level resource properties into a `resources` array

In Imposter 4, resource properties like `path`, `method` and `response` could be defined at the root level of a configuration file. In Imposter 5, these must be placed inside a `resources` array.

**Before (Imposter 4):**

```yaml
plugin: rest
path: /example
method: GET
response:
  file: example-data.json
```

**After (Imposter 5):**

```yaml
plugin: rest
resources:
- path: /example
  method: GET
  response:
    file: example-data.json
```

### 2. Rename `staticData` to `content`

The `staticData` response field has been renamed to `content`.

**Before:**

```yaml
response:
  staticData: "Hello, World!"
```

**After:**

```yaml
response:
  content: "Hello, World!"
```

### 3. Rename `staticFile` to `file`

The `staticFile` response field has been renamed to `file`.

**Before:**

```yaml
response:
  staticFile: data.json
```

**After:**

```yaml
response:
  file: data.json
```

### 4. Convert `scriptFile` to a step

The `scriptFile` response field has been replaced by the [steps](./steps.md) mechanism.

**Before:**

```yaml
response:
  scriptFile: handler.js
```

**After:**

```yaml
steps:
- type: script
  lang: javascript
  file: handler.js
```

### 5. Move `contentType` into response headers

The top-level `contentType` field has been replaced by the `Content-Type` response header.

**Before:**

```yaml
contentType: application/json
response:
  file: data.json
```

**After:**

```yaml
response:
  file: data.json
  headers:
    Content-Type: application/json
```

### 6. Use OpenAPI-style path parameters

Colon-prefixed path parameters (e.g. `:id`) must be changed to OpenAPI-style braces (e.g. `{id}`).

**Before:**

```yaml
path: /api/:version/users/:id
```

**After:**

```yaml
path: /api/{version}/users/{id}
```

## New: Configuration variables

Imposter 5 introduces a top-level `vars` section, letting you define values once and reference them inline with `${var.NAME}`. This is useful when migrating configs that previously repeated the same base path, host or identifier across multiple resources.

```yaml
plugin: rest
vars:
  api_base_path: /api/v2

resources:
- path: ${var.api_base_path}/users
  response:
    content: '{"ok": true}'

- path: ${var.api_base_path}/items
  response:
    content: '{"ok": true}'
```

See [Configuration variables](./configuration.md#configuration-variables) for the full syntax, including default values.

## OpenAPI remote schema references

In Imposter 4, the JVM engine resolved remote OpenAPI `$ref` references (over HTTP) at load time. In Imposter 5, remote reference resolution is **disabled by default**, because outbound HTTP from a mock server is undesirable in locked-down environments.

If your OpenAPI specifications rely on remote `$ref` resolution, you must explicitly opt in:

```shell
IMPOSTER_OPENAPI_ALLOW_REMOTE_REFS=true
```

## Legacy configuration support

If you want to use your existing Imposter 4 configuration files without modifying them, you can enable legacy configuration support by setting the following environment variable:

```shell
IMPOSTER_SUPPORT_LEGACY_CONFIG=true
```

When enabled, Imposter 5 automatically transforms legacy configuration at load time, applying all of the changes described above. This allows you to upgrade the engine without immediately updating your configuration files.

> **Note:** Legacy configuration support is intended as a transitional aid. It is recommended to update your configuration files to the current format when practical.

## Summary of changes

| Imposter 4 (legacy)       | Imposter 5 (current)                        |
|---------------------------|---------------------------------------------|
| Root-level `path`, `method`, `response` | `resources` array               |
| `response.staticData`     | `response.content`                          |
| `response.staticFile`     | `response.file`                             |
| `response.scriptFile`     | `steps` with `type: script`                 |
| `contentType`             | `response.headers.Content-Type`             |
| `:param` path parameters  | `{param}` path parameters                   |
| Groovy and JavaScript     | JavaScript only                             |
