# Proxy an existing endpoint

Imposter can generate mock configuration files for you by proxying an existing endpoint.

> If you don't have an existing endpoint to proxy, it's easy to create the configuration using [the guide](./configuration.md).

## Prerequisites

- Install the [Imposter CLI](./run_imposter_cli.md)
- An API/endpoint you wish to mock

## Steps

Let's imagine you want to proxy an endpoint running at `http://localhost:3000`. For simplicity, we will imagine it returns the following data:

    $ curl http://localhost:3000

    { "petType": "Cat", "petName": "Fluffy" }

Imposter can create a mock for you by acting as a proxy, capturing the request and responses as they are made.

### Create a mock

Start Imposter as a proxy:

    $ imposter proxy http://localhost:3000

    INFO[0000] starting proxy for upstream http://localhost:3000 on port 8080

> Imposter has started an HTTP server on port 8080. Any requests sent to `http://localhost:8080` will be forwarded to the upstream endpoint. Responses from the upstream will be recorded, then returned to the client. 

Now call the endpoint via the proxy:

    $ curl http://localhost:8080

    { "petType": "Cat", "petName": "Fluffy" }

You'll notice the response from the upstream endpoint is still returned, but in addition, Imposter **captured the important parts** of the request and response for you.

    DEBU[0003] received request GET / from client 127.0.0.1:53446
    DEBU[0003] invoking upstream http://localhost:3000 with GET / [body: 0 bytes]
    DEBU[0003] upstream responded to GET http://localhost:3000/ with status 200 [body 43 bytes]
    DEBU[0003] wrote response [status: 200, body 43 bytes] to client 127.0.0.1:53446
    DEBU[0003] proxied GET / to upstream [status: 200, body 43 bytes] for client 127.0.0.1:53446 in 1.82875ms
    DEBU[0003] wrote response file /Users/mary/example/GET-index.json for GET /index.json [43 bytes]
    DEBU[0003] wrote config file /Users/mary/example/localhost-3000-config.yaml for GET /index.json

Look in the directory where you started Imposter and you will see two new files:

    $ ls -l
    -rw-r--r--  1 mary  wheel    43B  8 Sep 13:56 GET-index.json
    -rw-r--r--  1 mary  wheel   171B  8 Sep 13:56 localhost-3000-config.yaml

The `GET-index.json` file contains the response body above. The `localhost-3000-config.yaml` is the Imposter configuration file.

### Testing the mock

Now we have our configuration and response files, we can stop the proxy and just use the mock.

Stop Imposter using `CTRL+C`.

In the same directory as the files you captured above, start Imposter:

    $ imposter up

    13:02:01 INFO  i.g.i.Imposter - Starting mock engine 3.0.4
    13:02:02 DEBUG i.g.i.c.u.ConfigUtil - Loading configuration file: /opt/imposter/config/localhost-3000-config.yaml
    13:02:03 DEBUG i.g.i.p.r.RestPluginImpl - Adding handler: GET -> /
    13:02:03 INFO  i.g.i.Imposter - Mock engine up and running on http://localhost:8080

Imposter read the configuration files and a mock of the original endpoint is now running at `http://localhost:8080`

Call the mock:

    $ curl http://localhost:8080

    { "petType": "Cat", "petName": "Fluffy" }

Imposter served the response based on what it captured.

    13:06:53 DEBUG i.g.i.h.AbstractResourceMatcher - Matched resource config for GET http://localhost:8080/
    13:06:53 INFO  i.g.i.s.ResponseServiceImpl - Serving response file GET-index.json for GET http://localhost:8080/ with status code 200

### Examine the mock

You can examine your mock by looking at the configuration file:

```yaml
# localhost-3000-config.yaml
plugin: rest
resources:
- method: GET
  path: /
  response:
    staticFile: GET-index.json
    statusCode: 200
    headers:
      Content-Type: application/json
```

Some things to note:

- The HTTP method and path from the request have been recorded by the proxy
- The `staticFile: GET-index.json` property refers to the JSON file representing the captured response body
- The response header (`Content-Type: application/json`) was also recorded

For completeness, see the contents of the response file reflect the content of the response body from the upstream endpoint:

```json
{ "petType": "Cat", "petName": "Fluffy" }
```

### Making changes

You can, of course, edit the configuration file so the mock behaves differently. When you change either the configuration file or response file, the Imposter CLI will restart to reflect your latest changes.

## What's next

Learn how to use Imposter with the [Configuration guide](configuration.md).
