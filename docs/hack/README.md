# Developing Imposter

## Convenience script

The script `scripts/dev-current.sh` is intended for local testing when changing the Impsoter codebase.

The script performs the following steps:

* builds Imposter from source
* starts Imposter using Docker or plain JVM
* enables a specified plugin
* passes the specified directory as the configuration directory

Usage:

```
dev-current.sh [args]

Arguments:

  -m <docker|java>  How to run Imposter, e.g. docker
  [-p plugin-name]  The plugin name, e.g. openapi
  [-c config-dir]   Fully qualified path to the Imposter configuration directory
  [-l log-level]    Logging level, e.g. DEBUG or TRACE
  [-t run-tests]    Whether to run tests (true or false). Default is true
```

Example:

```
$  ./scripts/dev-current.sh -m java -p openapi -c $PWD/docs/examples/openapi/simple
```

This starts Imposter in bare JVM mode (no Docker) with the OpenAPI plugin enabled, pointing to the examples directory.

### Debugging

When started using this script, JVM debug mode is enabled and the debug socket is opened on port 8000.

## Test with Gradle

You can run a test using a specific JVM version using Docker and Gradle as follows:

    docker run -it --rm -u gradle -v "$PWD":/home/gradle/project -w /home/gradle/project amd64/gradle:5.6-jdk8 gradle clean test
