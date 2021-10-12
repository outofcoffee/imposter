# Security

This section covers Imposter security. Topics include transport layer security (i.e. HTTPS) and authentication.

There are two primary approaches for adding TLS and authentication:

1. Using Imposter's embedded HTTP server
2. Using a reverse proxy or load balancer in front of Imposter

This section covers the first approach - using the embedded HTTP server. Using a reverse proxy or load balancer is a larger topic outside the scope of this documentation.

## TLS/SSL

You can run Imposter with HTTPS enabled. To do this, enable the TLS option and provide keystore options.

[Read more about how to enable TLS/SSL](./tls_ssl.md).

## Authentication

Imposter can require specific header values to authenticate incoming HTTP requests. To do this, use the `security` section within the plugin configuration file.

> Note: this example uses the [OpenAPI plugin](./openapi_plugin.md) but the same configuration works with [other plugins](./index.md) as well.

```yaml
# example-config.yaml
---
plugin: openapi
specFile: petstore.yaml

security:
  # no requests permitted by default
  default: Deny

  # only requests meeting these conditions are permitted
  conditions:
  - effect: Permit
    requestHeaders:
      Authorization: s3cr3t
```

### Concepts and terminology

Authentication configuration uses the following terms:

| Term      | Meaning                                                                     | Examples                           |
|-----------|-----------------------------------------------------------------------------|------------------------------------|
| Condition | A property of the request, such as the presence of a specific header value. | `Authorization` header value `foo` |
| Operator  | How the condition is matched.                                               | `EqualTo`, `NotEqualTo`            |
| Effect    | The impact of the condition on the request, such as it being denied.        | `Permit`, `Deny`                   |

The first important concept is the _Default Effect_. This is the effect that applies to all requests in the absence of a more specific condition. It is good practice to adhere the principle of least privilege. You can achieve this by setting the default effect to `Deny`, and then adding specific conditions that permit access.

```yaml
security:
  # no requests permitted by default
  default: Deny
```

This configuration will cause all responses from Imposter to have an `HTTP 401 Unauthorized` status.

Once you have configured the default effect, you typically add _Conditions_ to your security configuration, optionally specifying an _Operator_.

```yaml
security:
  # no requests permitted by default
  default: Deny

  # only requests meeting these conditions are permitted
  conditions:
  - effect: Permit
    requestHeaders:
      Authorization: s3cr3t
```

In this example, Imposter only permits requests that have the following HTTP request header:

```
Authorization: s3cr3t
```

Imposter will respond to these requests as normal, but respond to those without this specific header value with `HTTP 401 Unauthorized` status.  

The header name and value is arbitrary - you do not have to use the `Authorization` header. For example, you could specify:

```yaml
conditions:
- effect: Permit
  requestHeaders:
    X-Custom-Api-Key: s3cr3t
```

### Supported conditions

Imposter supports the following conditions:

| Condition        | Meaning                   | Type                 | Example                      |
|------------------|---------------------------|----------------------|------------------------------|
| `queryParams`    | Request query parameters. | Map of String:String | `{ "limit": "1" }`           |
| `requestHeaders` | Request headers.          | Map of String:String | `{ "Authorization": "foo" }` |

Here's an example showing all conditions:

```yaml
conditions:
- effect: Permit
  requestHeaders:
    X-Custom-Api-Key: s3cr3t

- effect: Deny
  requestHeaders:
    X-Forwarded-For:
      value: 1.2.3.4
      operator: NotEqualTo

- effect: Permit
  queryParams:
    apiKey:
      value: opensesame
      operator: EqualTo

- effect: Deny
  queryParams:
    apiKey: someblockedkey
```

#### Simple and extended form

For each condition you can use the simple form (`key: value`) or extended form, which allows customisation of matching behaviour.

The simple form for conditions is as follows:

```yaml
- effect: Permit
  queryParams:
    example: foo
```

If you want to control the logical operator you can use the extended form as follows:

```yaml
- effect: Permit
  queryParams:
    example:
      value: foo
      operator: NotEqualTo
```

By default, conditions are matched using the `EqualTo` operator.

Here, the value of the `example` query parameter is specified as a child property named `value`. The `operator` is also specified in this form, such as `EqualTo` or `NotEqualTo`.

### Combining conditions

The presence of more than one header in a condition requires all header values match in order for the condition to be satisfied.

```yaml
# requests are permitted if both headers match
conditions:
- effect: Permit
  requestHeaders:
    X-Custom-Api-Key: s3cr3t
    X-Another-Example: someothervalue
```

If you need different effects, use multiple conditions, as follows:

```yaml
# requests are permitted if both (1) and (2) are satisfied
conditions:
# (1) this header must match
- effect: Permit
  requestHeaders:
    X-Custom-Api-Key: s3cr3t

# (2) this header must not match
- effect: Deny
  requestHeaders:
    X-Another-Example: someothervalue
```

### Externalising values to environment variables

You can use environment variables to avoid including secrets in your configuration files. For example:

```yaml
conditions:
- effect: Permit
  requestHeaders:
    X-Custom-Api-Key: "${env.API_KEY}"
```

### Security and the status endpoint

Imposter has a status endpoint `/system/status` that is useful as a healthcheck.

When you apply a security policy with a default effect of `Deny`, it also applies to the status endpoint. This will cause requests to `/system/status` to be denied with `HTTP 401` status.

In cases where you want to permit traffic to the status endpoint without authentication, you can add the following configuration to your OpenAPI plugin or REST plugin configuration:

```yaml
# example-config.yaml
---
plugin: openapi
specFile: petstore.yaml

security:
  # no requests permitted by default
  default: Deny

resources:
  # always permit status endpoint
  - method: GET
    path: /system/status
    security:
      default: Permit
```

### More examples

See the `docs/examples` directory for working sample configurations, such as:

* [Simple authentication](https://github.com/outofcoffee/imposter/blob/master/docs/examples/openapi/authentication-simple)
* [Extended authentication](https://github.com/outofcoffee/imposter/blob/master/docs/examples/openapi/authentication-extended)
