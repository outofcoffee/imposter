# CORS

Imposter supports CORS ([Cross-Origin Resource Sharing](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)), which is a mechanism that allows a web page to make a request to a server in a different domain. This is useful for web applications that are hosted on a different domain to the mock endpoint they are consuming.

To configure CORS, add the following to your [configuration](./configuration.md):

```yaml
cors:
  allowOrigins: all
```

> **Warning**
> This will allow all origins to make requests to the mock endpoint. This is not recommended for production use.

## Specifying allowed origins

You can specify the allowed origins using the `allowOrigins` property. This is a list of strings, where each string is a domain or subdomain that is allowed to make requests to the mock endpoint.

```yaml
cors:
  allowOrigins:
    - http://localhost:8080
    - https://www.example.com 
```

### Special values

You can use the following special values for the `allowOrigins` property:

* `all` - allows all origins to make requests to the mock endpoint, echoing the value of the `Origin` header in the `Access-Control-Allow-Origin` response header. This effectively disables CORS.
* `*` - allows all origins to make requests to the mock endpoint, however, specific limitations such as use of `Access-Control-Allow-Credentials` apply. See [here](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#access-control-allow-origin) for more information.

## Specifying allowed headers

You can specify the allowed headers using the `allowHeaders` property. This is a list of strings, where each string is a header that is allowed to be sent in a request to the mock endpoint.

```yaml
cors:
  allowHeaders:
    - Content-Type
    - X-Custom-Header
```

## Specifying allowed methods

You can specify the allowed methods using the `allowMethods` property. This is a list of strings, where each string is an HTTP method that is allowed to be sent in a request to the mock endpoint.

```yaml
cors:
  allowMethods:
    - GET
    - POST
```

## Setting the max age

You can set the max age using the `maxAge` property. This is an integer value, representing the number of seconds that the browser should cache the CORS preflight response.

```yaml
cors:
  maxAge: 3600
```

## Allowing credentials

You can allow credentials to be sent in a request to the mock endpoint using the `allowCredentials` property. This is a boolean value.

```yaml
cors:
  allowCredentials: true
```

## Examples

- [cors-automatic](https://github.com/outofcoffee/imposter/blob/main/examples/rest/cors-automatic/)
