FROM eclipse-temurin:17-jdk-jammy AS build

# mirror uid/gid from distroless
RUN addgroup nonroot --gid 65532 && \
    adduser nonroot --uid 65532 --gid 65532

RUN mkdir -p \
    /opt/imposter/bin \
    /opt/imposter/lib \
    /opt/imposter/config \
    /opt/imposter/plugins \
    && chown -R nonroot:nonroot /opt/imposter

COPY --chown=nonroot:nonroot . /app
USER nonroot

WORKDIR /app

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
RUN ./gradlew :distro:distro-core:dist -xtest

RUN cp -r ./distro/core/build/install/imposter/lib/* /opt/imposter/lib/

FROM gcr.io/distroless/java17-debian12

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

ARG IMPOSTER_VERSION
ENV IMPOSTER_VERSION="${IMPOSTER_VERSION}" \
    IMPOSTER_ENGINE=unpacked \
    IMPOSTER_JVM_DISTRODIR=/opt/imposter

COPY --from=build --chown=nonroot:nonroot /opt/imposter /opt/imposter
USER nonroot

ENV IMPOSTER_PLUGIN_DIR=/opt/imposter/plugins

EXPOSE 8080 8443
ENTRYPOINT ["java", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher"]
CMD ["--plugin=openapi", "--plugin=rest", "--plugin=soap", "--configDir=/opt/imposter/config"]
