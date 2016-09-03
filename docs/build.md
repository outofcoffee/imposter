# Build

## Prerequisites

* JDK 8

## Steps

For distribution, Imposter is built as a 'fat JAR' (aka 'shadow JAR'). To get started with the examples here, first run:

    ./gradlew clean shadowJar

The JAR is created under the `distro/build/libs` directory.

If, instead, you want to compile the JAR without embedded dependencies, use:

    ./gradlew clean build

## Tests

If you want to run tests:

    ./gradlew clean test

## Docker containers

Build the Docker containers with:

    cd docker
    ./build.sh
