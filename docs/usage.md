# Usage (command line arguments and environment variables)

## Command line arguments

Some options can be controlled using command line arguments.

- If you are using the CLI, see [this documentation](https://github.com/imposter-project/imposter-cli/blob/main/README.md#usage) for a list of command line arguments.
- If you are using the JAR approach, see [this documentation](usage_jar.md).

## Environment variables

See [environment variables](./environment_variables.md) for a list of all environment variables.

## Setting environment variables using a file

You can use an environment file ('envfile') to pass environment variables to Imposter. To do this, add a file named `.env` adjacent to your configuration files, for example:

```
$ ls
.env
imposter-config.yaml

$ cat .env
IMPOSTER_LOG_LEVEL=info
OTHER_ENV_VAR=example
```
