# Percentage based error responses

This example has a roughly 50% chance of responding with an HTTP 200 or 500 status code.

The probability is set using a simple `script` step in the resource.

```js
if (Math.random() < 0.5) {
  respond().withStatusCode(500);
} else {
  respond().withStatusCode(200);
}
```

If more than one `example` exists for a response in the OpenAPI specification, the `respond().withExampleName('example-name-here')` function could be used.
