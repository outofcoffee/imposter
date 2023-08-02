# Failure simulation

Imposter can simulate failure types to enable you to test adverse scenarios, such as the connection being closed without receiving a response.

## Injecting failures

Failures can be injected via configuration or using a script driven approach.

The failure takes effect _after_ any request processing has completed (scripts, plugins etc.) but before the response is sent.

> **Note**
> Failures can also be combined with [performance simulation](./performance_simulation.md) effects. For example to simulate a delay followed by a closed connection.

The supported failure types are:

| Failure type     | Effect                                         | Configuration value |
|------------------|------------------------------------------------|---------------------|
| Empty response   | Send an empty HTTP response                    | `EmptyResponse`     |
| Close connection | Close the connection before sending a response | `CloseConnection`   |

### Configuration driven

Specify the `fail` option in configuration:

```yaml
# ...

resources:
  - method: GET
    path: /example1
    response:
      # send an empty HTTP response
      fail: EmptyResponse

  - method: GET
    path: /example2
    response:
      # close the connection before sending a response
      fail: CloseConnection
```

### Script driven

If using [Scripting](./scripting.md), use the `withFailure(...)` method.

To send an empty response:

```js
respond().withFailure('EmptyResponse')
```

To close the connection:

```js
respond().withFailure('CloseConnection')
```

### Logs

You will see log entries similar to the following:

```
15:39:09 INFO  i.g.i.s.CharacteristicsService - 
  Simulating EmptyResponse failure for GET http://localhost:50204/example1
```
