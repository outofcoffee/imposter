# Groovy JSON body parsing

Using the Groovy scritping engine, parse the JSON body of the HTTP request.

A working example:

```yaml
# imposter-config.yaml
plugin: rest

resources:
- path: /
  method: POST
  response:
    scriptFile: json-parse.groovy
```

```groovy
// json-parse.groovy

def parser = new groovy.json.JsonSlurper()
def json = parser.parseText(context.request.body)

respond().withContent(json.hello)
```

Example:

```shell
$ curl -X POST  http://localhost:8080 -d '{ "hello": "world" }'
...
world
```
