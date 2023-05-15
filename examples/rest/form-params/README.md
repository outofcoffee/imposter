# Matching form parameters

If you want to match form parameters, you can use `formParams` directive.

This works when the request has `Content-Type` header set to `application/x-www-form-urlencoded`.

## Example

Start engine:

```bash
$ imposter up
```

Send a request with encoded form parameters:

```bash
$ curl http://localhost:8080 --data-urlencode 'code=123'
hello world
```
