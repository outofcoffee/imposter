# Steps

Imposter can perform actions when it receives a request. These actions are called _Steps_; they take input, perform an operation, and, optionally, produce output.

Steps can call external systems, run scripts and use [stores](./stores.md). You can define as many steps as you need. Steps are executed in the order they are defined in the configuration file.

Steps can use data from the current request, access stores, or use the output of previous steps. Step outputs can be used in mock responses, such as in a [response template](./templates.md), or to drive conditional logic.

## Step Types

Imposter supports the following step types:

- Execute a script
- Make an HTTP(S) request to a server

### Execute a script

The `script` step type allows you to execute a script. The script has access to the request context and stores. Script code can be inline or in an external file.

> **Note**
> See the [scripting](./scripting.md) section for more information about writing scripts.

#### Inline script

Here is an example of an inline script.

> **Note**
> Inline scripts can be written in JavaScript or Groovy.
> Set the `lang` property to either `javascript` or `groovy`

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: script
      lang: javascript
      code: console.log('Hello World!');
```

##### Explanation
Calling the endpoint `/example` with a `GET` request will cause the inline JavaScript code to be executed.

The code prints `Hello World!` to the console.

##### Multiline inline scripts

The `code` property can support multi-line YAML strings, such as:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: script
      lang: javascript
      code: |
        console.log('Hello World!');
        console.log('This is a second line');
```

#### External script

Scripts can be stored in an external file. Here is an example of an external script.

> **Note**
> External scripts can be written in JavaScript or Groovy.

Let's assume you have a file named `example.js` with the following content:

```javascript
console.log('Hello World!');
```

Your step configuration would look like this:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: script
      file: example.js
```

##### Explanation
Calling the endpoint `/example` with a `GET` request will cause the external JavaScript file `example.js` to be executed.

The code prints `Hello World!` to the console.

---

### Make an HTTP(S) request to a server

The `remote` step type allows you to make an HTTP(S) request to a server. The response, including headers and body, can be in [response templates](templates.md), captured in a [store](./stores.md) or used in later steps.

#### Example: Send a request

Here is an example of a configuration file with a simple `remote` step:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: remote
      url: http://example.com
      method: GET
```

##### Explanation
Calling the endpoint `/example` with a `GET` request will cause Imposter to send an HTTP request to `http://example.com`.

Since no `capture` section is defined, the response will not be stored.

#### Example: Send a request and store the response

Here is an example of a configuration file with a `remote` step that captures the response body:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    - type: remote
      url: http://example.com
      method: GET
      capture:
        responseBody:
          expression: "${remote.response.body}"

  response:
    content: "The upstream response was: ${stores.request.responseBody}"
```

##### Explanation
Calling the endpoint `/example` with a `GET` request will cause Imposter to send an HTTP request to `http://example.com`.

Since a `capture` block is defined, the response status code and body will be held in the `request` store.

As with all [stores](./stores.md), you can use data in [response templates](./templates.md), scripts, or access the data in subsequent steps.

In this example, the mock response will contain the body of the response from `http://example.com` prefixed with the string `The upstream response was:`.

##### Multiple captures

You can capture multiple parts of the response by adding more properties to the `capture` block.

For example, to capture the status code and the response body:

```yaml
# ...part of your configuration file
capture:
  statusCode:
    expression: "${remote.response.statusCode}"
  responseBody:
    expression: "${remote.response.body}"
```

In this example, both the `statusCode` and `responseBody` properties will be stored in the `request` store. You can then use these properties in subsequent steps or in response templates, by referencing the store properties, such as `${stores.request.statusCode}`.

---

## Further examples

This section contains further examples of using steps, including multiple steps and using stores.

### Example: Use previous step output in a later step

Here is an example of a configuration file with multiple steps:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    # the first step will send an HTTP request to example.com
    # and store its response status code and body in the 'request' store
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
      lang: javascript
      code: |
        console.log('Remote HTTP response status code: ' + stores.request.statusCode);
```

> **Note**
> Remember that the `request` store is ephemeral, and holds values for the current, in-flight request. See the [Stores documentation](./stores) for details.

### Example: Use previous step output in the mock response

Here is an example of a configuration file with multiple steps:

```yaml
# ...part of your configuration file

resources:
- path: /example
  method: GET
  steps:
    # send an HTTP request to example.com
    # and store its response status code and body in the 'request' store
    - type: remote
      url: http://example.com
      method: GET
      capture:
        statusCode:
          expression: "${remote.response.statusCode}"
        responseBody:
          expression: "${remote.response.body}"
  
  # the mock response will be the content of the response body
  # returned by the call to `http://example.com`
  response:
    content: "${stores.request.responseBody}"
```

> **Note**
> Remember that the `request` store is ephemeral, and holds values for the current, in-flight request. See the [Stores documentation](./stores) for details.
