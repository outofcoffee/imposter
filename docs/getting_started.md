# Getting started

Note: See the [Usage](docs/usage.md) section for the required arguments, and the examples below.

## Running as a Docker container

The easiest way to get started is to use an Imposter Docker container, such as:

    docker run -ti -p 8443:8443 outofcoffee/imposter-rest [args]

### Docker images

The following images are available:

* `outofcoffee/imposter-rest`
* `outofcoffee/imposter-openapi`
* `outofcoffee/imposter-hbase`
* `outofcoffee/imposter-sfdc`

_Note:_ There is also a base container that does not enable any plugins:

    outofcoffee/imposter

You can use the base image to create your own custom images.

### Example

If you want to run Imposter using Docker, use:

    docker run -ti -p 8443:8443 \
        -v /path/to/config:/opt/imposter/config \
        outofcoffee/imposter-rest [args]

...ensuring that you choose the right image for the plugin you wish to use.

## Running as a standalone Java application

If Docker isn't your thing, or you want to build Imposter yourself, you can create a standlone JAR file.
See the [Build](build.md) section.

Once, built, you can run the JAR as follows:

    java -jar distro/build/libs/imposter.jar \
        --plugin <plugin class> \
        --configDir <config dir> \
        [args]

...ensuring that you choose the right plugin class for the plugin you want to use, for example:

    java -jar distro/build/libs/imposter.jar \
        --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
        --configDir /path/to/config \
        [args]
