# Groovy tips and tricks

## Background

This section provides additional tips and tricks when using Groovy as the scripting language for your mocks. It builds on the [Scripting](./scripting.md) documentation. If you are new to Imposter scripting, it's best to start there.

## Block syntax for `respond`

Groovy users can use this special block syntax, for improved readability:

```groovy
respond {
    // behaviours go here
}
```

For example:

```groovy
respond {
    withStatusCode 201
    and()
    usingDefaultBehaviour()
}
```

## Parsing JSON

To parse JSON in the request, you can use the Groovy `JsonSlurper` class in your scripts.

Here's a working example:

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

respond().withData(json.hello)
```

Example:

```shell
$ curl -X POST  http://localhost:8080 -d '{ "hello": "world" }'
...
world
```

## Dynamically loading other Groovy scripts

You can dynamically include another Groovy script file in your script code.

> **Warning:** An important consideration with dynamic script inclusion is performance. Normally, when Imposter sees a script file referenced from config, it precompiles it on startup, so that executions of the script are faster. Depending on machine resources, script size, and other factors script execution can be 10x-100x slower without precompilation.

If you want to include Groovy scripts dynamically, then you can do something like this:

```groovy
// entrypoint.groovy

def other = loadDynamic('/path/to/other-script.groovy')

respond().withStatusCode(other.getStatusCode())
```

then in the referenced file you'd have something like:

```groovy
// other-script.groovy
// this file is included from 'entrypoint.groovy'

int getStatusCode() {
    return 201
}
```

Note that the result of calling the `loadDynamic(..)` method is a new Groovy script object. Specifically, the `GroovyClassLoader` returns a subclass of `groovy.lang.Script`.

This object does not share global state ('bindings') with the calling script, so it doesn't have access to DSL functions like `respond()` etc.

A work-around is use the dynamically-loaded scripts to do computation/logic and return an object for your dispatcher to then use when it calls `respond()`.
