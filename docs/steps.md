# Steps

Imposter can perform multiple operations when it receives a request. These are called Steps. You can add multiple steps to a resource, calling external systems, running scripts and using [stores](./stores.md). Steps can use the output of other steps, as well as data from the request, or stores. The response can use the outputs of steps, such as in a [response template](./templates.md).

## Step Types

Imposter supports the following step types:

- Execute a script
- Make an HTTP(S) request to a server

> **Note**
> Steps are executed in the order they are defined in the configuration file.

### Simple example

Here is an example with a single step:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: script
      script: |
        console.log('Hello World!');
```

### Multiple steps

Here is an example of a configuration file with multiple steps:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:

  # the first step will send an HTTP request to example.com
  # and store its response status code and body in the 'current' object
  - type: remote
    url: http://example.com
    method: GET
    capture:
      statusCode:
        expression: "${remote.response.statusCode}"
      responseBody:
        expression: "${remote.response.body}"

  # the second step will log the status code of the response
  - type: script
    script: |
      console.log('Remote HTTP response status code: ' + current.statusCode);
  
  response:
    content: "${current.responseBody}"
```
