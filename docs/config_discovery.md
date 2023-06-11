# Configuration discovery

By default, configuration files with the suffix `-config.json`, `-config.yaml` or `-config.yml` are loaded from the configuration directory.

## Recursively loading configuration files

By default, Imposter reads configuration files within the configuration directories, but not their subdirectories.

To also load configuration files within subdirectories, set the following environment variable:

    IMPOSTER_CONFIG_SCAN_RECURSIVE="true"

This will load all configuration files from the configuration directory and all subdirectories.

### Recursive config scan performance

> **Warning**
> When using recursive scan for configuration, directories with many files or subdirectories (such as `node_modules`) can significantly slow down Imposter startup time.

Typically, you would not store your Imposter configuration files within a directory such as `node_modules`, so it is safe to ignore such paths when scanning for configuration files. The mechanism for this is a file named `.imposterignore`

When this file is placed in the root of a configuration directory, you can customise which files/directories to ignore by exact match. For example:

```
# By default, ignore the following files and directories
# when searching for config files

.git
.idea
.svn
node_modules
```

If no `.imposterignore` file is present in any configuration directory, a default ignore file is used containing sensible defaults.

---

## Prefixing all paths using the `basePath` property

It is possible to prefix all paths in a configuration file using the `basePath` property.

This can be set at the root of the configuration file and applies to all `path` properties in that configuration file.

Example:

```yaml
plugin: rest
basePath: /example

resources:
- path: /foo
  method: GET
  response:
    content: "Hello world"

- path: /bar
  method: GET
  response:
    content: "Good day"
```

This would result in the resources being accessible at the path: `/example/foo` and `/example/bar`, rather than `/foo` and `/bar`.

## Automatic base path, when using recursive configuration discovery

When using recursive configuration discovery, you can automatically set the `basePath` based on the subdirectory path.

To enable this behaviour set the environment variable:

```
IMPOSTER_AUTO_BASE_PATH=true
```

If the directory structure was as follows:

```
<config root>
\-- mock1
|   \-- some-config.yaml
\-- mock2
|   \-- another-config.yaml
\-- nested
    \-- mock2
        \-- another-config.yaml
```

...then the `basePath` will be set as follows:

- for 'mock1' it would be `/mock1`
- for 'mock2' it would be `/mock2`
- for 'mock3' it would be `/nested/mock3`
