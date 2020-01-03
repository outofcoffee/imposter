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

## Hacking locally

You can build and run locally using the `dev-current.sh` convenience script. This will start Imposter in debug mode, with the debug port running on 8000.
    
Build and run with Docker:
    
    ./scripts/dev-current.sh -m docker

Build and run with local JVM:

    ./scripts/dev-current.sh -m java

More complete example specifying plugin and config directory:

     ./scripts/dev-current.sh -m docker -p rest -c $(pwd)/docs/examples/rest/multiple 

## Embedding in your application

To embed Imposter in your application see [this section](embed.md).
