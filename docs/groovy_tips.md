# Groovy tips and tricks

## Background

This section provides additional tips and tricks when using Groovy as the scripting language for your mocks. It builds on the [Scripting](./scripting.md) documentation. If you are new to Imposter scripting, it's best to start there.

## Debugging Groovy scripts

See the [debugging Groovy scripts](./groovy_debugging.md) documentation.

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

respond().withContent(json.hello)
```

Example:

```shell
$ curl -X POST  http://localhost:8080 -d '{ "hello": "world" }'
...
world
```

## Producing JSON strings

To produce JSON from an object, use the `JsonOutput.toJson` method.

Here's an example:

```groovy
def obj = [ hello: 'world' ]
def json = groovy.json.JsonOutput.toJson(obj)
        
respond().withContent(json)
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

## Adding a JAR file to the classpath for Groovy scripts

You can add a JAR file to the classpath for Groovy scripts by adding it to the plugins directory. This will be added to the classpath when the Groovy script is executed.

To do this:

1. create a directory containing the JAR file
2. set the `IMPOSTER_PLUGIN_DIR` environment variable (or set `plugin.dir` in your `.imposter.yaml` file in the config directory) to the path above

## Improved Groovy DSL support in your IDE

For more sophisticated script development, you can set up a project in your IDE with improved code assistance. You can use this alongside [the debugger](./groovy_debugging.md) for a fully integrated development experience.

This requires setting up a project with a dependency on `imposter-api.jar` and adding the GroovyDSL (GDSL) file.

➡️ [See an example project](https://github.com/imposter-project/examples/tree/main/groovy-dsl)

If you just want to download the GDSL file see:

➡️ [Download the GDSL file](https://github.com/imposter-project/examples/tree/main/groovy-dsl/src/main/resources/imposter.gdsl) 
