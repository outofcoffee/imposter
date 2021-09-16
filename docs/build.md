# Build

## Hacking locally

You can build and run locally using the `dev-current.sh` convenience script. This will start Imposter in debug mode, with the debug port running on 8000.

Build and run with Docker:

    ./scripts/dev-current.sh -m docker

Build and run with local JVM:

    ./scripts/dev-current.sh -m java

More complete example specifying plugin and config directory:

     ./scripts/dev-current.sh -m docker -p rest -c $PWD/docs/examples/rest/multiple

> See the [README](./hack/README.md) for more details about using this script.

## Local build

If you don't want to use the convenience script, then you can follow these steps.

### Prerequisites

* JDK 8

### Steps

For distribution, Imposter is built as an all-in-one JAR file. This is available as a Docker image, as well as in raw form.

To get started with the examples here, first run:

    ./gradlew clean shadowJar

The JAR is created under the `distro/all/build/libs` directory.

> If, instead, you wanted to compile the JAR _without_ embedded dependencies, use:
>
>     ./gradlew clean build

## Tests

If you want to run tests:

    ./gradlew clean test

## Docker containers

Build the Docker containers with:

    ./scripts/docker-build.sh

---

## Extending to build a custom application

To extend Imposter and build a custom application see [this section](extend.md).