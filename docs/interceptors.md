# Interceptors

Interceptors are a useful way to execute logic early in the request-response process. They are executed before resources, allowing you to short-circuit a response, capture data or run a script to compute data for subsequent use in a resource. Interceptors are somewhat analogous to 'middleware' in Node.js/Express or Java Servlet interceptors.

Interceptors match requests using the same [matching rules](request_matching.md) as resources. This allows you to apply interceptors to particular requests.

Multiple interceptors can be executed for a single request if they run in passthrough mode.

## Short-circuit a response

Interceptors can respond to a request, short-circuiting the response and preventing resources from being served.

For example, you might wish to check a property of the request, and respond with an HTTP 400 status if it does not match a particular value.

```yaml
plugin: rest

interceptors:
  - path: /*
    requestHeaders:
      User-Agent:
        value: Some-User-Agent
        operator: NotEqualTo
    response:
      statusCode: 400
      content: Invalid user agent

resources:
  - path: /example
    method: GET
    response:
      content: "hello world"
```

### Explanation

In this example, an interceptor runs on all requests (note the path `/*`) and responds with `HTTP 400` if the user agent request header does not match an expected value. Doing this in an interceptor avoids having to duplicate this check in each of the resources.

## Passthrough

Interceptors can run in 'passthrough' mode. Request processing continues after the interceptor has finished. This enables a resource to serve a response after an interceptor has completed its work.

For example, you might want to capture a property of the request and make it available to all resources. You could also run a script to compute some data on each request before your resources serve a response.

```yaml
plugin: rest

interceptors:
  - path: /*
    continue: true
    capture:
      theUserAgent:
        requestHeader: "user-agent"

resources:
  - path: /example1
    method: GET
    response:
      template: true
      content: |
        Example one
        User agent: ${stores.request.theUserAgent}

  - path: /example2
    method: GET
    response:
      template: true
      content: |
        Example two
        User agent: ${stores.request.theUserAgent}
```

### Explanation

In this example, an interceptor runs on all requests (note the path `/*`) and [captures](./data_capture.md) the user agent request header for use by the resources. Note the `continue: true` property in the interceptor. This indicates that request processing should continue to the `resources` after the interceptor has completed.

The data captured by the interceptor is used by the resources in their response.

## Multiple interceptors

Multiple interceptors can be executed for a single request if they run in passthrough mode.

```yaml
plugin: rest

interceptors:
  - path: /*
    continue: true
    capture:
      theUserAgent:
        requestHeader: "user-agent"

  - path: /*
    steps:
      - type: script
        code: |
          var req = stores.open("request");
          req.save("otherData", "foo");
          respond().continueToNext();

resources:
  - path: /example
    method: GET
    response:
      template: true
      content: |
        User agent: ${stores.request.theUserAgent}
        Other data: ${stores.request.otherData}
```

### Explanation

In this example, two interceptors execute for every request. The first sets the `continue: true` property, which indicates request processing should continue after it has completed.

The second interceptor has a [script step](./steps.md). Within the script, `respond().continueToNext()` is called, which has the same effect as setting `continue: true` in the configuration.

Both interceptors store data, which is later used by the `/example` resource.

## Examples

See the following examples:

- See [examples/rest/interceptors-simple](https://github.com/imposter-project/examples/blob/main/rest/interceptors-simple) for a short-circuit example.
- See [examples/rest/interceptors-passthrough](https://github.com/imposter-project/examples/blob/main/rest/interceptors-passthrough) for a passthrough example.
