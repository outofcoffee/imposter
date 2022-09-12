# Performance simulation

Characteristics like response latency can be simulated.

## Injecting delays

Delays can be injected via a configuration driven approach or a script driven approach. Delays are specified in milliseconds.

The delay controls the time added _after_ any request processing has completed (scripts, plugins etc.). So, if a normal un-delayed request takes 0.05 seconds and a delay of 100ms is added, then the response will be transmitted to the client after 105ms.

Delays can be specified as an exact value, or a range. If a range is specified, then a random value (roughly uniformly distributed) will be selected between the minimum and maximum range values.

### Configuration driven

Specify the `delay` section in configuration:

For the root resource:

```yaml
# ...

response:
  statusCode: 200
  file: "response.json"
  
  # exactly 1000ms delay
  delay:
    exact: 1000
```

For a specific resource (e.g. OpenAPI or REST plugin):

```yaml
# ...

resources:
  - method: GET
    path: /example
    response:
      statusCode: 200
      file: "response.json"
      
      # delay in range of 500ms-1500ms
      delay:
        min: 500
        max: 1500
```

### Script driven

If using [Scripting](./scripting.md), use the `withDelay(exactDelay)` and `withDelayRange(minDelay, maxDelay)` methods:

For an exact delay:

```js
respond()
    .withStatusCode(200)
    .withFile('response.json')
    .withDelay(1000)
```

For a delay within a range:

```js
respond()
    .withStatusCode(200)
    .withFile('response.json')
    .withDelayRange(500, 1500)
```

### Logs

You will see log entries similar to the following:

```
14:39:08 INFO  i.g.i.s.ResponseServiceImpl - 
  Delaying mock response for GET /example-range-delay by 1000ms

14:39:09 INFO  i.g.i.s.ResponseServiceImpl - 
  Serving response data (5 bytes) for URI http://localhost:50203/example-range-delay with status code 200
```
