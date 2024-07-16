# Using legacy JavaScript engine (Nashorn)

The [default JavaScript engine is GraalVM](./scripting_modern_js.md), which is based on ECMAScript 2022 (more formally, [ECMA-262, 13th edition](https://262.ecma-international.org/13.0/)). Whilst GraalVM provides support for modern JavaScript features, it is more resource intensive than the legacy Nashorn JavaScript engine, which only supports ECMAScript 5 (ES5). You can switch to the Nashorn JavaScript engine using its plugin.

To use the Nashorn JavaScript engine, you need to be running Imposter v4.0.0 or later, and install the `js-nashorn` plugin.

## Install plugin

### Option 1: Using the CLI

> **Note**
> This option requires the [Imposter CLI](./run_imposter_cli.md) version 0.37.0 or later.

To use this plugin, install it with the Imposter CLI:

    imposter plugin install -d js-nashorn

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Install the plugin manually

To use this plugin, download the `imposter-plugin-js-nashorn.jar` JAR file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Enable it with the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Using the plugin

To use Nashorn, you need to specify the `js-nashorn` engine as the JavaScript plugin. You can do this by setting the environment variable `IMPOSTER_JS_PLUGIN` to `js-nashorn`:

```bash
export IMPOSTER_JS_PLUGIN=js-nashorn
```

---

## Example

> **Note**
> Complete the prerequisites first.

Start the mock server with the `js-nashorn` engine:

```bash
imposter up examples/rest/conditional-scripted -e IMPOSTER_JS_PLUGIN=js-nashorn
```

Send a request to the mock server:

```bash
curl -i http://localhost:8080/pets

[
  {
    "id": 1,
    "name": "Fluffy"
  },
  {
    "id": 2,
    "name": "Paws"
  }
]
```

* See the `examples/rest/conditional-scripted` directory [in GitHub](https://github.com/outofcoffee/imposter/blob/main/examples/rest/conditional-scripted).
