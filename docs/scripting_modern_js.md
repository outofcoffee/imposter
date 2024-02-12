# Using modern JavaScript in scripts

The default JavaScript engine is Nashorn, which is based on ECMAScript 5.1. However, you can use modern JavaScript features by using the GraalVM JavaScript engine.

### Features

GraalVM enables you to use modern JavaScript features such as:

- `let` and `const` for variable declarations
- Arrow functions
- Template literals
- Destructuring
- Classes

To use the GraalVM JavaScript engine, you need to install the `js-graal` plugin.

## Install plugin

### Option 1: Using the CLI

> **Note**
> This option requires the [Imposter CLI](./run_imposter_cli.md) version 0.37.0 or later.

To use this plugin, install it with the Imposter CLI:

    imposter plugin install -d js-graal:zip

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Using the JAR

To use this plugin, download the plugin `imposter-plugin-js-graal.zip` ZIP file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Enable it with the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Using the plugin

To use GraalVM, you need to specify the `js-graal` engine as the JavaScript plugin. You can do this by setting the environment variable `IMPOSTER_JS_PLUGIN` to `js-graal`:

```bash
export IMPOSTER_JS_PLUGIN=js-graal
```

---

## Examples

For examples, see the `examples/graal` directory [in GitHub](https://github.com/outofcoffee/imposter/blob/main/examples/graal).

> **Note**
> Complete the prerequisites first.

### Simple example

Start the mock server with the `js-graal` engine:

```bash
imposter up examples/graal/simple -e IMPOSTER_JS_PLUGIN=js-graal
```

Send a request to the mock server:

```bash
curl -i http://localhost:8080?name=Ada

Hello Ada
```

### Advanced example

See the `examples/graal/es6` directory for an example of using modern JavaScript features in a script.
