# Build

## Prerequisites

* JDK 8

## Steps

For distribution, Imposter is built as a 'fat JAR' (aka 'shadow JAR'). To get started with the examples here, first run:

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
