FROM gradle:7.6-jdk17 AS build

# mirror uid/gid from distroless
RUN addgroup nonroot --gid 65532 && \
    adduser nonroot --uid 65532 --gid 65532

COPY --chown=nonroot:nonroot . /app
USER nonroot
WORKDIR /app

RUN mkdir -p \
    ./build/imposter/bin \
    ./build/imposter/lib \
    ./build/imposter/config \
    ./build/imposter/plugins

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

# don't run shadowJar/dist tasks
RUN gradle :distro:distro-core:installDist -xtest

RUN cp -r ./distro/core/build/install/imposter/lib/* ./build/imposter/lib/

FROM gcr.io/distroless/java17-debian12

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

ARG IMPOSTER_VERSION
ENV IMPOSTER_VERSION="${IMPOSTER_VERSION}" \
    IMPOSTER_ENGINE=unpacked \
    IMPOSTER_JVM_DISTRODIR=/opt/imposter

COPY --from=build --chown=nonroot:nonroot /app/build/imposter /opt/imposter
USER nonroot

ENV IMPOSTER_PLUGIN_DIR=/opt/imposter/plugins

EXPOSE 8080 8443
ENTRYPOINT ["java", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher"]
CMD ["--plugin=openapi", "--plugin=rest", "--plugin=soap", "--configDir=/opt/imposter/config"]
